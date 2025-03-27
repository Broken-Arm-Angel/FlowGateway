package com.dpeng.gateway.core.filter.loadbalance.strategy;

import com.dpeng.gateway.config.pojo.ServiceInstance;
import com.dpeng.gateway.core.context.GatewayContext;

import java.util.List;

import static com.dpeng.gateway.common.constant.LoadBalanceConstant.CLIENT_IP_LOAD_BALANCE_STRATEGY;

/**
 * 基于客户端 IP 的普通哈希负载均衡策略
 */
public class ClientIpLoadBalanceStrategy implements LoadBalanceStrategy{

    @Override
    public ServiceInstance selectInstance(GatewayContext context, List<ServiceInstance> instances) {
        return instances.get(Math.abs(context.getRequest().getHost().hashCode()) % instances.size());
    }

    @Override
    public String mark() {
        return CLIENT_IP_LOAD_BALANCE_STRATEGY;
    }

}
