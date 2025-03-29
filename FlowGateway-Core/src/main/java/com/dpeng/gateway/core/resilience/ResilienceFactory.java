package com.dpeng.gateway.core.resilience;

import cn.hutool.core.collection.ConcurrentHashSet;
import com.dpeng.gateway.common.enums.CircuitBreakerEnum;
import com.dpeng.gateway.config.config.manager.DynamicConfigManager;
import com.dpeng.gateway.config.pojo.RouteDefinition;
import io.github.resilience4j.bulkhead.*;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ResilienceFactory {

    //Map<String, T>：这些 Map 用来存储每个服务的不同 Resilience4j 组件实例，T 分别代表 Retry、CircuitBreaker、Bulkhead 和 ThreadPoolBulkhead，服务名 serviceName 作为键。
    private static final Map<String, Retry> retryMap = new ConcurrentHashMap<>();
    private static final Map<String, CircuitBreaker> circuitBreakerMap = new ConcurrentHashMap<>();
    private static final Map<String, Bulkhead> bulkheadMap = new ConcurrentHashMap<>();
    private static final Map<String, ThreadPoolBulkhead> threadPoolBulkheadMap = new ConcurrentHashMap<>();

    //Set<String>：这些 Set 用来跟踪服务是否已经被添加到配置中，防止重复配置监听
    private static final Set<String> retrySet = new ConcurrentHashSet<>();
    private static final Set<String> circuitBreakerSet = new ConcurrentHashSet<>();
    private static final Set<String> bulkheadSet = new ConcurrentHashSet<>();
    private static final Set<String> threadPoolBulkheadSet = new ConcurrentHashSet<>();

    //为服务构建一个重试机制，支持 最大重试次数 和 重试间隔 的配置。当路由配置变更时，移除对应服务的 Retry 配置。
    public static Retry buildRetry(RouteDefinition.ResilienceConfig resilienceConfig, String serviceName) {
        if (!resilienceConfig.isRetryEnabled()) {
            return null;// 如果没有启用重试，直接返回 null
        }
        return retryMap.computeIfAbsent(serviceName, name -> {
            if (!retrySet.contains(serviceName)) {// 如果该服务没有被监听
                DynamicConfigManager.getInstance().addRouteListener(serviceName, newRoute -> retryMap.remove(newRoute.getServiceName()));
                retrySet.add(serviceName);
            }
            //RetryConfig 只是一个通用的配置模板，可以用于多个 Retry 实例, 默认是一个map
            RetryConfig config = RetryConfig.custom()
                    .maxAttempts(resilienceConfig.getMaxAttempts()) // 设置最大重试次数
                    .waitDuration(Duration.ofMillis(resilienceConfig.getWaitDuration())) // 设置重试间隔时间
                    .build();
            return RetryRegistry.of(config).retry(serviceName);// 注册一个serviceName的默认Retry实例并返回该实例
        });
    }

    //为服务创建熔断器，配置 失败率、慢调用阈值、半开状态下的请求数 等。监听路由变化，移除旧的熔断器配置。
    public static CircuitBreaker buildCircuitBreaker(RouteDefinition.ResilienceConfig resilienceConfig, String serviceName) {
        if (!resilienceConfig.isCircuitBreakerEnabled()) {
            return null;// 如果没有启用熔断，返回 null
        }
        return circuitBreakerMap.computeIfAbsent(serviceName, name -> {
            if (!circuitBreakerSet.contains(serviceName)) {
                DynamicConfigManager.getInstance().addRouteListener(serviceName, newRoute -> circuitBreakerMap.remove(newRoute.getServiceName()));
                circuitBreakerSet.add(serviceName);
            }
            CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                    .failureRateThreshold(resilienceConfig.getFailureRateThreshold())//失败率阈值（%），当请求失败率 超过 该值时，熔断器会进入 开启（Open） 状态。
                    .slowCallRateThreshold(resilienceConfig.getSlowCallRateThreshold())//慢调用比例阈值（%），当慢调用（耗时超过 slowCallDurationThreshold）的比例 超过 该值时，熔断器也会开启。
                    .waitDurationInOpenState(Duration.ofMillis(resilienceConfig.getWaitDurationInOpenState()))//熔断器开启后的等待时间，即熔断器在 开启（Open） 状态后，多久 自动进入半开（Half-Open） 状态尝试恢复。
                    .slowCallDurationThreshold(Duration.ofSeconds(resilienceConfig.getSlowCallDurationThreshold()))//慢调用时间阈值，大于这个值的请求 被认为是慢调用，并影响 slowCallRateThreshold 的计算。
                    .permittedNumberOfCallsInHalfOpenState(resilienceConfig.getPermittedNumberOfCallsInHalfOpenState())//	半开状态允许的请求数，熔断器进入 半开（Half-Open） 状态时，最多允许多少个请求 尝试恢复。
                    .minimumNumberOfCalls(resilienceConfig.getMinimumNumberOfCalls())//	最小调用次数，在 滑动窗口期内，至少需要这么多调用后，才会开始计算失败率。
                    .slidingWindowType(slidingWindowTypeConvert(resilienceConfig.getType()))//滑动窗口类型，有两种模式： TIME_BASED：基于时间窗口统计失败率。 COUNT_BASED：基于请求数量统计失败率。
                    .slidingWindowSize(resilienceConfig.getSlidingWindowSize())//滑动窗口大小，决定了 计算失败率时的观察范围，如果是 TIME_BASED，代表时间长度（秒）；如果是 COUNT_BASED，代表请求次数。
                    .build();
            return CircuitBreakerRegistry.of(circuitBreakerConfig).circuitBreaker(serviceName);
        });
    }

    //为服务创建 信号量隔离，用于限制并发请求数，防止资源过载。监听路由变化，监听路由变化，移除旧的信号量配置。
    public static Bulkhead buildBulkHead(RouteDefinition.ResilienceConfig resilienceConfig, String serviceName) {
        if (!resilienceConfig.isBulkheadEnabled()) {
            return null;
        }
        return bulkheadMap.computeIfAbsent(serviceName, name -> {
            if (!bulkheadSet.contains(serviceName)) {
                DynamicConfigManager.getInstance().addRouteListener(serviceName, newRoute -> bulkheadMap.remove(newRoute.getServiceName()));
                bulkheadSet.add(serviceName);
            }
            BulkheadConfig bulkheadConfig = BulkheadConfig.custom()
                    .maxConcurrentCalls(resilienceConfig.getMaxConcurrentCalls())//	最大并发请求数，即允许多少个线程 同时 访问受保护的资源。
                    .maxWaitDuration(Duration.ofMillis(resilienceConfig.getMaxWaitDuration()))//最大等待时间，当所有并发槽位都被占用时，新的请求最多可以等待多久（如果超时，则直接失败）。
                    .fairCallHandlingStrategyEnabled(resilienceConfig.isFairCallHandlingEnabled()).build();//是否启用公平调度，如果启用，则请求 按顺序 进入执行队列，否则 无序竞争。
            return BulkheadRegistry.of(bulkheadConfig).bulkhead(serviceName);
        });
    }

    //为服务创建 线程池隔离，通过配置 核心线程数、最大线程数、队列容量 来限制并发处理。
    public static ThreadPoolBulkhead buildThreadPoolBulkhead(RouteDefinition.ResilienceConfig resilienceConfig, String serviceName) {
        if (!resilienceConfig.isThreadPoolBulkheadEnabled()) {
            return null;
        }
        return threadPoolBulkheadMap.computeIfAbsent(serviceName, name -> {
            if (!threadPoolBulkheadSet.contains(serviceName)) {
                DynamicConfigManager.getInstance().addRouteListener(serviceName, newRoute -> threadPoolBulkheadMap.remove(newRoute.getServiceName()));
                threadPoolBulkheadSet.add(serviceName);
            }
            ThreadPoolBulkheadConfig threadPoolBulkheadConfig = ThreadPoolBulkheadConfig.custom()
                    .coreThreadPoolSize(resilienceConfig.getCoreThreadPoolSize())
                    .maxThreadPoolSize(resilienceConfig.getMaxThreadPoolSize())
                    .queueCapacity(resilienceConfig.getQueueCapacity())
                    .build();
            return ThreadPoolBulkheadRegistry.of(threadPoolBulkheadConfig).bulkhead(serviceName);
        });
    }

    /**
     * 作用：将 CircuitBreakerEnum 转换为 Resilience4j 中的滑动窗口类型。滑动窗口有两种类型：
     * 时间基（TIME_BASED）：基于时间的窗口。
     * 计数基（COUNT_BASED）：基于请求次数的窗口。
     */
    private static CircuitBreakerConfig.SlidingWindowType slidingWindowTypeConvert(CircuitBreakerEnum from) {
        if (from == CircuitBreakerEnum.TIME_BASED) {
            return CircuitBreakerConfig.SlidingWindowType.TIME_BASED;
        } else {
            return CircuitBreakerConfig.SlidingWindowType.COUNT_BASED;
        }
    }

}
