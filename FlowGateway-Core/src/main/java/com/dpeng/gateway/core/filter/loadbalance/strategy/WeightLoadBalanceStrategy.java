package com.dpeng.gateway.core.filter.loadbalance.strategy;

import com.dpeng.gateway.config.pojo.ServiceInstance;
import com.dpeng.gateway.core.context.GatewayContext;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static com.dpeng.gateway.common.constant.LoadBalanceConstant.WEIGHT_LOAD_BALANCE_STRATEGY;

/**
 * 加权随机负载均衡
 */
public class WeightLoadBalanceStrategy implements LoadBalanceStrategy {

    @Override
    public ServiceInstance selectInstance(GatewayContext context, List<ServiceInstance> instances) {
        // 计算所有服务实例的权重总和
        int totalWeight = instances.stream().mapToInt(ServiceInstance::getWeight).sum();

        // 如果总权重小于等于 0，说明没有可用实例，返回 null
        if (totalWeight <= 0) return null;

        // 生成一个 [0, totalWeight) 之间的随机数
        int randomWeight = ThreadLocalRandom.current().nextInt(totalWeight);

        // 遍历实例列表，按照权重进行选择
        for (ServiceInstance instance : instances) {
            randomWeight -= instance.getWeight();
            if (randomWeight < 0) return instance;
        }
        return null;
    }

    @Override
    public String mark() {
        return WEIGHT_LOAD_BALANCE_STRATEGY;
    }

}
