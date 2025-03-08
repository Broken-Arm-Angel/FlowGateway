package com.dpeng.gateway.core.context;

import com.dpeng.gateway.config.pojo.RouteDefinition;
import com.dpeng.gateway.core.filter.FilterChain;
import com.dpeng.gateway.core.helper.ContextHelper;
import com.dpeng.gateway.core.request.GatewayRequest;
import com.dpeng.gateway.core.response.GatewayResponse;
import io.netty.channel.ChannelHandlerContext;
import lombok.Data;

@Data
public class GatewayContext {

    /**
     * Netty上下文
     */
    private ChannelHandlerContext nettyCtx;

    /**
     * 请求过程中发生的异常
     */
    private Throwable throwable;

    private GatewayRequest request;

    private GatewayResponse response;

    private RouteDefinition route;

    private boolean keepAlive;

    private FilterChain filterChain;

    /**
     * 当前执行到第几个过滤器
     */
    private int curFilterIndex = 0;

    /**
     * 标识是否执行 前置过滤器
     */
    private boolean isDoPreFilter = true;

    public GatewayContext(ChannelHandlerContext nettyCtx, GatewayRequest request,
                          RouteDefinition route, boolean keepAlive) {
        this.nettyCtx = nettyCtx;
        this.request = request;
        this.route = route;
        this.keepAlive = keepAlive;
    }

    public void doFilter() {
        int size = filterChain.size();
        if (isDoPreFilter) {
            filterChain.doPreFilter(curFilterIndex++, this);
            if (curFilterIndex == size) {
                isDoPreFilter = false;
                curFilterIndex--;
            }
        } else {
            filterChain.doPostFilter(curFilterIndex--, this);
            if (curFilterIndex < 0) {
                ContextHelper.writeBackResponse(this);
            }
        }
    }

}
