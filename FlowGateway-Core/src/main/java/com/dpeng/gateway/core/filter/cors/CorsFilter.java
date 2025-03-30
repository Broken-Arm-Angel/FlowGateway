package com.dpeng.gateway.core.filter.cors;

import com.dpeng.gateway.common.enums.ResponseCode;
import com.dpeng.gateway.core.context.GatewayContext;
import com.dpeng.gateway.core.filter.Filter;
import com.dpeng.gateway.core.helper.ContextHelper;
import com.dpeng.gateway.core.helper.ResponseHelper;
import com.dpeng.gateway.core.response.GatewayResponse;
import io.netty.handler.codec.http.HttpMethod;

import static com.dpeng.gateway.common.constant.FilterConstant.CORS_FILTER_NAME;
import static com.dpeng.gateway.common.constant.FilterConstant.CORS_FILTER_ORDER;

/**
 * 跨域处理过滤器
 */
public class CorsFilter implements Filter {

    @Override
    public void doPreFilter(GatewayContext context) {
        //判断请求方法是否为 OPTIONS，如果是，则认为是跨域请求的预检。
        if (HttpMethod.OPTIONS.equals(context.getRequest().getMethod())) {
            //如果是预检请求，直接构建一个成功的响应 (ResponseCode.SUCCESS)，并设置跨域相关的响应头。
            context.setResponse(ResponseHelper.buildGatewayResponse(ResponseCode.SUCCESS));
            ContextHelper.writeBackResponse(context);//直接将响应写回给客户端，不再经过后续的过滤器。
        } else {
            //如果请求方法不是 OPTIONS，则继续调用 context.doFilter()，交给下一个过滤器处理。
            context.doFilter();
        }
    }

    //
    @Override
    public void doPostFilter(GatewayContext context) {
        //处理实际请求（如 GET, POST 等）之后的响应，添加 CORS 响应头。
        GatewayResponse gatewayResponse = context.getResponse();
        gatewayResponse.addHeader("Access-Control-Allow-Origin", "*");//允许哪些来源的请求。* 表示允许所有来源。
        gatewayResponse.addHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");// 允许的 HTTP 请求方法（如 GET, POST, PUT 等）。
        gatewayResponse.addHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");//允许客户端发送的请求头
        gatewayResponse.addHeader("Access-Control-Allow-Credentials", "true");//表示是否允许浏览器发送带有认证信息的请求，true 表示允许。
        context.doFilter();
    }

    @Override
    public String mark() {
        return CORS_FILTER_NAME;
    }

    @Override
    public int getOrder() {
        return CORS_FILTER_ORDER;
    }

}
