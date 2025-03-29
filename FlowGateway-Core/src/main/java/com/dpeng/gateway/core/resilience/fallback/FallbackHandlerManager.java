package com.dpeng.gateway.core.resilience.fallback;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * 加载和管理所有的降级处理器 (FallbackHandler)，并根据需求返回相应的降级处理器实例。
 */
@Slf4j
public class FallbackHandlerManager {

    private static final Map<String, FallbackHandler> handlerMap = new HashMap<>();

    static {
        ServiceLoader<FallbackHandler> serviceLoader = ServiceLoader.load(FallbackHandler.class);
        for (FallbackHandler handler : serviceLoader) {
            handlerMap.put(handler.mark(), handler);
            log.info("load fallback handler success: {}", handler);
        }
    }

    public static FallbackHandler getHandler(String name) {
        FallbackHandler handler = handlerMap.get(name);
        if (handler == null)
             handler = new DefaultFallbackHandler();
        return handler;
    }

}
