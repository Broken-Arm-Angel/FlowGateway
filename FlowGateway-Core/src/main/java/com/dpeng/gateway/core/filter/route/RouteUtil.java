package com.dpeng.gateway.core.filter.route;

import com.dpeng.gateway.core.context.GatewayContext;
import com.dpeng.gateway.core.helper.ResponseHelper;
import com.dpeng.gateway.core.http.HttpClient;
import org.asynchttpclient.Request;
import org.asynchttpclient.Response;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

public class RouteUtil {

    //Supplier 懒加载（调用 get() 才会执行），便于在 Resilience 机制（重试、熔断等）中使用。
    public static Supplier<CompletionStage<Response>> buildRouteSupplier(GatewayContext context) {
        return () -> {
            //从 GatewayContext 中获取 HTTP 请求对象，并调用 .build() 构造完整请求。
            Request request = context.getRequest().build();

            //通过 单例 HttpClient 发送 HTTP 请求，返回 CompletableFuture<Response>（异步执行）。
            CompletableFuture<Response> future = HttpClient.getInstance().executeRequest(request);
            future.whenComplete(((response, throwable) -> {
                if (throwable != null) {
                    context.setThrowable(throwable);
                    throw new RuntimeException(throwable);
                }
                context.setResponse(ResponseHelper.buildGatewayResponse(response));
                context.doFilter();
            }));
            return future;
        };
    }

}
