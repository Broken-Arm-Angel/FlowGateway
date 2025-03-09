package com.dpeng.gateway.core.filter.gray.strategy;


import com.dpeng.gateway.config.pojo.RouteDefinition;
import com.dpeng.gateway.config.pojo.ServiceInstance;
import com.dpeng.gateway.config.util.FilterUtil;
import com.dpeng.gateway.core.context.GatewayContext;

import java.util.List;

import static com.dpeng.gateway.common.constant.FilterConstant.GRAY_FILTER_NAME;
import static com.dpeng.gateway.common.constant.GrayConstant.CLIENT_IP_GRAY_STRATEGY;

/**
 * 根据用户ip决定是否灰度
 */
public class ClientIpGrayStrategy implements GrayStrategy {

    @Override
    public boolean shouldRoute2Gray(GatewayContext context, List<ServiceInstance> instances) {
        if (instances.stream().anyMatch(instance -> instance.isEnabled() && !instance.isGray())) {
            RouteDefinition.GrayFilterConfig grayFilterConfig = FilterUtil.findFilterConfigByClass(context.getRoute().getFilterConfigs(), GRAY_FILTER_NAME, RouteDefinition.GrayFilterConfig.class);
            double grayThreshold = instances.stream().mapToDouble(ServiceInstance::getThreshold).sum();
            grayThreshold = Math.min(grayThreshold, grayFilterConfig.getMaxGrayThreshold());
            return Math.abs(context.getRequest().getHost().hashCode()) % 100 <= grayThreshold * 100;
        }
        return true;
    }

    @Override
    public String mark() {
        return CLIENT_IP_GRAY_STRATEGY;
    }

}
