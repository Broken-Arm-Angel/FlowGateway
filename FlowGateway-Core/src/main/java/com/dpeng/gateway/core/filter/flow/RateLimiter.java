package com.dpeng.gateway.core.filter.flow;


import com.dpeng.gateway.core.context.GatewayContext;

public interface RateLimiter {

    void tryConsume(GatewayContext context);

}
