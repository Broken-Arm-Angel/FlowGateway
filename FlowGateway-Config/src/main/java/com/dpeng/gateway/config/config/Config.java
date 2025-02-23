package com.dpeng.gateway.config.config;

import com.dpeng.gateway.common.enums.ConfigCenterEnum;
import com.dpeng.gateway.common.enums.RegisterCenterEnum;
import lombok.Data;
import java.util.List;


/**
 * 网关配置
 */
@Data
public class Config {

    // base
    private int port = 9999;
    private String env = "dev";

    // 配置中心
    private ConfigCenterEnum configCenter = ConfigCenterEnum.NACOS; // 配置中心实现
    private String configAddress = "127.0.0.1:8848"; // 配置中心地址

    // 注册中心
    private RegisterCenterEnum registerCenter = RegisterCenterEnum.NACOS; // 注册中心实现
    private String registerAddress = "127.0.0.1:8848"; // 注册中心地址

    //netty
    private int eventLoopGroupBossNum = 1;
    private int eventLoopGroupWorkerNum = Runtime.getRuntime().availableProcessors();
    private int maxContentLength = 64 * 1024 * 1024; // 64MB

    // 路由配置
    private List<RouteDefinition> routes;

}
