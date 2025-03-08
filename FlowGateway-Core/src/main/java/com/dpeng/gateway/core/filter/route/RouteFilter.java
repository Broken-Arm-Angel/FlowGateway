package com.dpeng.gateway.core.filter.route;

import com.dpeng.gateway.common.enums.ResponseCode;
import com.dpeng.gateway.config.pojo.RouteDefinition;
import com.dpeng.gateway.core.context.GatewayContext;
import com.dpeng.gateway.core.filter.Filter;
import com.dpeng.gateway.core.helper.ContextHelper;
import com.dpeng.gateway.core.helper.ResponseHelper;
import com.dpeng.gateway.core.resilience.Resilience;
import org.asynchttpclient.Response;

import java.util.concurrent.CompletableFuture;

import static com.dpeng.gateway.common.constant.FilterConstant.ROUTE_FILTER_NAME;
import static com.dpeng.gateway.common.constant.FilterConstant.ROUTE_FILTER_ORDER;

public class RouteFilter implements Filter {

    @Override
    public void doPreFilter(GatewayContext context) {
        RouteDefinition.ResilienceConfig resilience = context.getRoute().getResilience();
        if (resilience.isEnabled()) { // 开启弹性配置
            Resilience.getInstance().executeRequest(context);
        } else {
            CompletableFuture<Response> future = RouteUtil.buildRouteSupplier(context).get().toCompletableFuture();
            future.exceptionally(throwable -> {
                context.setResponse(ResponseHelper.buildGatewayResponse(ResponseCode.HTTP_RESPONSE_ERROR));
                ContextHelper.writeBackResponse(context);
                return null;
            });
        }
    }

    @Override
    public void doPostFilter(GatewayContext context) {
        context.doFilter();
    }

    @Override
    public String mark() {
        return ROUTE_FILTER_NAME;
    }

    @Override
    public int getOrder() {
        return ROUTE_FILTER_ORDER;
    }

}
