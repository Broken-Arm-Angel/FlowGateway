package com.dpeng.gateway.core.filter.gray.strategy;


import com.dpeng.gateway.config.pojo.ServiceInstance;
import com.dpeng.gateway.core.context.GatewayContext;

import java.util.List;

public interface GrayStrategy {

    boolean shouldRoute2Gray(GatewayContext context, List<ServiceInstance> instances);

    String mark();

}
