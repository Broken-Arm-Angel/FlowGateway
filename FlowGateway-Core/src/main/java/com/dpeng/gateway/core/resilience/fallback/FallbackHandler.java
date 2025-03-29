package com.dpeng.gateway.core.resilience.fallback;

import com.dpeng.gateway.core.context.GatewayContext;

public interface FallbackHandler {

    void handle(Throwable throwable, GatewayContext context);

    String mark();

}
