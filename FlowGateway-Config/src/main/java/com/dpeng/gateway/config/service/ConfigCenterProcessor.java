package com.dpeng.gateway.config.service;


import com.dpeng.gateway.config.config.ConfigCenter;

/**
 * 配置中心接口
 */
public interface ConfigCenterProcessor {

    /**
     * 初始化配置中心配置
     */
    void init(ConfigCenter configCenter);

    /**
     * 订阅配置中心配置变更
     */
    void subscribeRoutesChange(RoutesChangeListener listener);

}
