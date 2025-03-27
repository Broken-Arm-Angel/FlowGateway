package com.dpeng.gateway.core.filter.loadbalance.strategy;

import com.dpeng.gateway.config.pojo.ServiceInstance;
import com.dpeng.gateway.core.context.GatewayContext;

import java.util.List;

public interface LoadBalanceStrategy {

    ServiceInstance selectInstance(GatewayContext context, List<ServiceInstance> instances);

    String mark();

}
