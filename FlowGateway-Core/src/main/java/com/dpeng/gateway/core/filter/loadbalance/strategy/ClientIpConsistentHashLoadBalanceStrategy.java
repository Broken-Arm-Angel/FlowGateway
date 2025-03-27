package com.dpeng.gateway.core.filter.loadbalance.strategy;

import com.dpeng.gateway.config.pojo.RouteDefinition;
import com.dpeng.gateway.config.pojo.ServiceInstance;
import com.dpeng.gateway.config.util.FilterUtil;
import com.dpeng.gateway.core.algorithm.ConsistentHashing;
import com.dpeng.gateway.core.context.GatewayContext;

import java.util.List;

import static com.dpeng.gateway.common.constant.FilterConstant.LOAD_BALANCE_FILTER_NAME;
import static com.dpeng.gateway.common.constant.LoadBalanceConstant.CLIENT_IP_CONSISTENT_HASH_LOAD_BALANCE_STRATEGY;

/**
 * 基于客户端 IP 的一致性哈希负载均衡策略
 */
public class ClientIpConsistentHashLoadBalanceStrategy implements LoadBalanceStrategy {

    @Override
    public ServiceInstance selectInstance(GatewayContext context, List<ServiceInstance> instances) {
        //从路由配置中读取负载均衡参数。
        RouteDefinition.LoadBalanceFilterConfig loadBalanceFilterConfig = FilterUtil.findFilterConfigByClass(context.getRoute().getFilterConfigs(), LOAD_BALANCE_FILTER_NAME, RouteDefinition.LoadBalanceFilterConfig.class);
        int virtualNodeNum = 1;
        if (loadBalanceFilterConfig != null && loadBalanceFilterConfig.getVirtualNodeNum() > 0) {
            virtualNodeNum = loadBalanceFilterConfig.getVirtualNodeNum();
        }

        //将所有实例 ID 组成 nodes 列表，用于创建一致性哈希环。
        List<String> nodes = instances.stream().map(ServiceInstance::getInstanceId).toList();
        ConsistentHashing consistentHashing = new ConsistentHashing(nodes, virtualNodeNum);

        //从 context 中获取客户端的 Host（通常是 IP), 用host的hash值作为key, 通过哈希环得到目标实例ID
        String selectedNode = consistentHashing.getNode(String.valueOf(context.getRequest().getHost().hashCode()));
        for (ServiceInstance instance : instances) {
            if (instance.getInstanceId().equals(selectedNode)) {
                return instance;
            }
        }

        //兜底策略：如果 selectedNode 对应的实例不在 instances 列表中（例如，该实例临时不可用），则默认返回 第一个实例。
        return instances.get(0);
    }

    @Override
    public String mark() {
        return CLIENT_IP_CONSISTENT_HASH_LOAD_BALANCE_STRATEGY;
    }

}
