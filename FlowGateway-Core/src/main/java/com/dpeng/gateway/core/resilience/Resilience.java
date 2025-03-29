package com.dpeng.gateway.core.resilience;
import com.dpeng.gateway.common.enums.ResilienceEnum;
import com.dpeng.gateway.common.enums.ResponseCode;
import com.dpeng.gateway.config.pojo.RouteDefinition;
import com.dpeng.gateway.core.context.GatewayContext;
import com.dpeng.gateway.core.filter.route.RouteUtil;
import com.dpeng.gateway.core.helper.ContextHelper;
import com.dpeng.gateway.core.helper.ResponseHelper;
import com.dpeng.gateway.core.resilience.fallback.FallbackHandler;
import com.dpeng.gateway.core.resilience.fallback.FallbackHandlerManager;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.ThreadPoolBulkhead;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import org.asynchttpclient.Response;

import java.util.concurrent.*;
import java.util.function.Supplier;

/**
 * Resilience（弹性机制），用于在 API 网关 中提供 重试（Retry）、熔断（CircuitBreaker）、信号量隔离（Bulkhead）、线程池隔离（ThreadPoolBulkhead）、降级（Fallback） 等容错策略，确保高可用性。
 */
public class Resilience {

    // 单例模式，保证全局唯一 Resilience 实例。
    private static final Resilience INSTANCE = new Resilience();

    //用于调度 异步重试任务 的线程池
    ScheduledExecutorService retryScheduler = Executors.newScheduledThreadPool(10);

    private Resilience() {
    }

    public static Resilience getInstance() {
        return INSTANCE;
    }

    //核心方法，执行请求，并根据配置应用 弹性策略（Resilience）。
    public void executeRequest(GatewayContext gatewayContext) {
        RouteDefinition.ResilienceConfig resilienceConfig = gatewayContext.getRoute().getResilience();
        String serviceName = gatewayContext.getRequest().getServiceDefinition().getServiceName();

        //构造请求的执行逻辑，封装成 Supplier，后续可以用 重试、熔断、限流等策略包装。
        Supplier<CompletionStage<Response>> supplier = RouteUtil.buildRouteSupplier(gatewayContext);

        //按照指定顺序 依次应用不同的 Resilience 机制。
        for (ResilienceEnum resilienceEnum : resilienceConfig.getOrder()) {
            switch (resilienceEnum) {
                //重试
                case RETRY -> {
                    Retry retry = ResilienceFactory.buildRetry(resilienceConfig, serviceName);
                    if (retry != null) {
                        //用重试机制装饰异步任务, 同时触发重试机制的线程为传入的线程池来调用
                        supplier = Retry.decorateCompletionStage(retry, retryScheduler, supplier);
                    }
                }
                //降级
                case FALLBACK -> {
                    if (resilienceConfig.isFallbackEnabled()) {
                        Supplier<CompletionStage<Response>> finalSupplier = supplier;
                        supplier = () ->
                                finalSupplier.get().exceptionally(throwable -> {
                                    //请求失败时的降级逻辑
                                    FallbackHandler handler = FallbackHandlerManager.getHandler(resilienceConfig.getFallbackHandlerName());
                                    handler.handle(throwable, gatewayContext);
                                    return null;
                                });
                    }
                }
                //熔断
                case CIRCUITBREAKER -> {
                    CircuitBreaker circuitBreaker = ResilienceFactory.buildCircuitBreaker(resilienceConfig, serviceName);
                    if (circuitBreaker != null) {
                        //熔断机制装饰异步任务
                        supplier = CircuitBreaker.decorateCompletionStage(circuitBreaker, supplier);
                    }
                }
                //信号量隔离
                case BULKHEAD -> {
                    Bulkhead bulkhead = ResilienceFactory.buildBulkHead(resilienceConfig, serviceName);
                    if (bulkhead != null) {
                        supplier = Bulkhead.decorateCompletionStage(bulkhead, supplier);
                    }
                }
                //线程池隔离
                case THREADPOOLBULKHEAD -> {
                    ThreadPoolBulkhead threadPoolBulkhead = ResilienceFactory.buildThreadPoolBulkhead(resilienceConfig, serviceName);
                    if (threadPoolBulkhead != null) {
                        Supplier<CompletionStage<Response>> finalSupplier = supplier;
                        supplier = () -> {
                            // 执行异步任务并将其交给线程池进行处理
                            CompletionStage<CompletableFuture<Response>> future =
                                    threadPoolBulkhead.executeSupplier(() -> finalSupplier.get().toCompletableFuture());
                            try {
                                // 等待线程池执行任务的结果并返回
                                return future.toCompletableFuture().get();
                            } catch (InterruptedException | ExecutionException e) {
                                // 异常处理，如果线程被中断或执行出现异常，抛出运行时异常
                                throw new RuntimeException(e);
                            }
                        };
                    }
                }
            }
        }

        //执行最终请求，如果 Fallback 没有启用，就直接返回错误响应。
        supplier.get().exceptionally(throwable -> {
            if (!resilienceConfig.isFallbackEnabled()) {
                gatewayContext.setThrowable(throwable);
                gatewayContext.setResponse(ResponseHelper.buildGatewayResponse(ResponseCode.SERVICE_UNAVAILABLE));
                ContextHelper.writeBackResponse(gatewayContext);
            }
            return null;
        });
    }

}
