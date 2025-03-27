package com.dpeng.gateway.core.filter.loadbalance;

import cn.hutool.json.JSONUtil;
import com.dpeng.gateway.common.enums.ResponseCode;
import com.dpeng.gateway.common.exception.NotFoundException;
import com.dpeng.gateway.config.config.manager.DynamicConfigManager;
import com.dpeng.gateway.config.pojo.RouteDefinition;
import com.dpeng.gateway.config.pojo.ServiceInstance;
import com.dpeng.gateway.config.util.FilterUtil;
import com.dpeng.gateway.core.context.GatewayContext;
import com.dpeng.gateway.core.filter.Filter;
import com.dpeng.gateway.core.filter.loadbalance.strategy.GrayLoadBalanceStrategy;
import com.dpeng.gateway.core.filter.loadbalance.strategy.LoadBalanceStrategy;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import static com.dpeng.gateway.common.constant.FilterConstant.LOAD_BALANCE_FILTER_NAME;
import static com.dpeng.gateway.common.constant.FilterConstant.LOAD_BALANCE_FILTER_ORDER;

@Slf4j
public class LoadBalanceFilter implements Filter {

    @Override
    public void doPreFilter(GatewayContext context) {
        RouteDefinition.FilterConfig filterConfig = FilterUtil.findFilterConfigByName(context.getRoute().getFilterConfigs(), LOAD_BALANCE_FILTER_NAME);
        if (filterConfig == null) {
            filterConfig = FilterUtil.buildDefaultLoadBalanceFilterConfig();
        }
        // 获取服务所有实例
        List<ServiceInstance> instances = DynamicConfigManager.getInstance()
                .getInstancesByServiceName(context.getRequest().getServiceDefinition().getServiceName())
                .values().stream().toList();

        LoadBalanceStrategy strategy;
        //如果请求是灰度,则筛选出所有灰度实例,选择灰度负载均衡策略, 确保灰度流量只进入灰度实例; 如果是普通请求,则从负载均衡配置中读取策略
        if (context.getRequest().isGray()) {
            strategy = new GrayLoadBalanceStrategy(); // 灰度负载均衡策略
            // 如果请求是灰度的，再进行一遍过滤
            instances = instances.stream().filter(instance -> instance.isEnabled() && instance.isGray()).toList();
        } else {
            strategy = selectLoadBalanceStrategy(JSONUtil.toBean(filterConfig.getConfig(), RouteDefinition.LoadBalanceFilterConfig.class));
        }
        if (instances.isEmpty()) {
            throw new NotFoundException(ResponseCode.SERVICE_INSTANCE_NOT_FOUND);
        }
        ServiceInstance serviceInstance = strategy.selectInstance(context, instances);
        if (null == serviceInstance) {
            throw new NotFoundException(ResponseCode.SERVICE_INSTANCE_NOT_FOUND);
        }
        // 设置负载均衡后下游的目标服务器host
        context.getRequest().setModifyHost(serviceInstance.getIp() + ":" + serviceInstance.getPort());
        context.doFilter();
    }

    @Override
    public void doPostFilter(GatewayContext context) {
        context.doFilter();
    }

    @Override
    public String mark() {
        return LOAD_BALANCE_FILTER_NAME;
    }

    @Override
    public int getOrder() {
        return LOAD_BALANCE_FILTER_ORDER;
    }

    private LoadBalanceStrategy selectLoadBalanceStrategy(RouteDefinition.LoadBalanceFilterConfig loadBalanceFilterConfig) {
        return LoadBalanceStrategyManager.getStrategy(loadBalanceFilterConfig.getStrategyName());
    }

}
