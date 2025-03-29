package com.dpeng.gateway.core.resilience.fallback;
import com.dpeng.gateway.common.enums.ResponseCode;
import com.dpeng.gateway.core.context.GatewayContext;
import com.dpeng.gateway.core.helper.ContextHelper;
import com.dpeng.gateway.core.helper.ResponseHelper;

import static com.dpeng.gateway.common.constant.FallbackConstant.DEFAULT_FALLBACK_HANDLER_NAME;

/**
 * 默认的降级处理器
 */
public class DefaultFallbackHandler implements FallbackHandler {

    @Override
    public void handle(Throwable throwable, GatewayContext context) {
        context.setThrowable(throwable);
        context.setResponse(ResponseHelper.buildGatewayResponse(ResponseCode.GATEWAY_FALLBACK));
        ContextHelper.writeBackResponse(context);
    }

    @Override
    public String mark() {
        return DEFAULT_FALLBACK_HANDLER_NAME;
    }

}
