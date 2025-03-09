package com.dpeng.gateway.core.filter.gray.strategy;

import com.dpeng.gateway.config.pojo.RouteDefinition;
import com.dpeng.gateway.config.pojo.ServiceInstance;
import com.dpeng.gateway.config.util.FilterUtil;
import com.dpeng.gateway.core.context.GatewayContext;

import java.util.List;

import static com.dpeng.gateway.common.constant.FilterConstant.GRAY_FILTER_NAME;
import static com.dpeng.gateway.common.constant.GrayConstant.MAX_GRAY_THRESHOLD;
import static com.dpeng.gateway.common.constant.GrayConstant.THRESHOLD_GRAY_STRATEGY;

/**
 * 根据流量决定是否灰度策略
 */
public class ThresholdGrayStrategy implements GrayStrategy {


    /**
     * 通过计算灰度实例的总比例，并在不超过最大阈值的情况下，按概率判断该请求是否进入灰度版本。
     * @param context 网关上下文
     * @param instances 服务实例 ,Threshold 服务实例期望接收的灰度流量占比
     * @return 是否进入灰度
     */
    @Override
    public boolean shouldRoute2Gray(GatewayContext context, List<ServiceInstance> instances) {
        //如果至少存在一个非灰度实例，则进行 灰度阈值计算
        //如果 所有实例都是灰度，直接返回 true（100% 进入灰度）
        if (instances.stream().anyMatch(instance -> instance.isEnabled() && !instance.isGray())) {
            RouteDefinition.GrayFilterConfig grayFilterConfig = FilterUtil.findFilterConfigByClass(context.getRoute().getFilterConfigs(), GRAY_FILTER_NAME, RouteDefinition.GrayFilterConfig.class);
            double maxGrayThreshold = grayFilterConfig == null ? MAX_GRAY_THRESHOLD : grayFilterConfig.getMaxGrayThreshold();
            double grayThreshold = instances.stream().mapToDouble(ServiceInstance::getThreshold).sum();
            grayThreshold = Math.min(grayThreshold, maxGrayThreshold);
            return Math.abs(Math.random() - 1) <= grayThreshold;
        }
        return true;
    }

    @Override
    public String mark() {
        return THRESHOLD_GRAY_STRATEGY;
    }

}
