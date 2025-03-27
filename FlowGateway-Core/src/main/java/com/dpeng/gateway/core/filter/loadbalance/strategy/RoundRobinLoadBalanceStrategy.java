package com.dpeng.gateway.core.filter.loadbalance.strategy;

import com.dpeng.gateway.config.pojo.RouteDefinition;
import com.dpeng.gateway.config.pojo.ServiceInstance;
import com.dpeng.gateway.config.util.FilterUtil;
import com.dpeng.gateway.core.context.GatewayContext;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static com.dpeng.gateway.common.constant.FilterConstant.LOAD_BALANCE_FILTER_NAME;
import static com.dpeng.gateway.common.constant.LoadBalanceConstant.ROUND_ROBIN_LOAD_BALANCE_STRATEGY;

/**
 * 轮询负载均衡策略, 支持 严格轮询 和 非严格轮询 模式, 前者多线程下是严格的顺序轮询, 后者每个线程有自己的轮询顺序
 */
public class RoundRobinLoadBalanceStrategy implements LoadBalanceStrategy {

    // 存储每个服务名称对应的严格轮询位置（使用AtomicInteger保证线程安全）
    Map<String, AtomicInteger> strictPositionMap = new ConcurrentHashMap<>();

    // 存储每个服务名称对应的非严格轮询位置
    Map<String, Integer> positionMap = new ConcurrentHashMap<>();

    // 安全阈值，防止位置值过大，触发轮询重置
    private final int THRESHOLD = Integer.MAX_VALUE >> 2; // 预防移除的安全阈值

    @Override
    public ServiceInstance selectInstance(GatewayContext context, List<ServiceInstance> instances) {
        // 默认使用严格轮询
        boolean isStrictRoundRobin = true;

        // 获取负载均衡配置，判断是否启用严格轮询
        RouteDefinition.LoadBalanceFilterConfig loadBalanceFilterConfig = FilterUtil.findFilterConfigByClass(context.getRoute().getFilterConfigs(), LOAD_BALANCE_FILTER_NAME, RouteDefinition.LoadBalanceFilterConfig.class);
        if (loadBalanceFilterConfig != null) {
            isStrictRoundRobin = loadBalanceFilterConfig.isStrictRoundRobin();
        }

        // 获取当前请求的服务名
        String serviceName = context.getRequest().getServiceDefinition().getServiceName();
        ServiceInstance serviceInstance;

        if (isStrictRoundRobin) {
            // 严格轮询模式，使用AtomicInteger保证线程安全
            AtomicInteger strictPosition = strictPositionMap.computeIfAbsent(serviceName, k -> new AtomicInteger(0));
            // 获取并更新轮询位置
            int index = Math.abs(strictPosition.getAndIncrement());
            serviceInstance = instances.get(index % instances.size());
            // 如果轮询位置超过阈值，则重置
            if (index >= THRESHOLD) {
                strictPosition.set((index + 1) % instances.size());
            }
        } else {
            // 非严格轮询模式，使用普通的Integer来记录位置
            int position = positionMap.getOrDefault(serviceName, 0);
            int index = Math.abs(position++);
            serviceInstance = instances.get(index % instances.size());
            if (position >= THRESHOLD) {
                positionMap.put(serviceName, (position + 1) % instances.size());
            }
        }
        return serviceInstance;
    }

    @Override
    public String mark() {
        return ROUND_ROBIN_LOAD_BALANCE_STRATEGY;
    }

}
