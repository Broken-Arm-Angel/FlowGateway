package com.dpeng.gateway.core.filter.loadbalance.strategy;


import com.dpeng.gateway.config.pojo.ServiceInstance;
import com.dpeng.gateway.core.context.GatewayContext;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static com.dpeng.gateway.common.constant.LoadBalanceConstant.RANDOM_LOAD_BALANCE_STRATEGY;

/**
 * 负载均衡策略: 随机
 */
public class RandomLoadBalanceStrategy implements LoadBalanceStrategy {

    @Override
    public ServiceInstance selectInstance(GatewayContext context, List<ServiceInstance> instances) {
        return instances.get(ThreadLocalRandom.current().nextInt(instances.size()));
    }

    @Override
    public String mark() {
        return RANDOM_LOAD_BALANCE_STRATEGY;
    }

}
