package com.dpeng.gateway.config.service.impl.nacos;

import com.alibaba.fastjson.JSON;
import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import com.dpeng.gateway.config.config.ConfigCenter;
import com.dpeng.gateway.config.config.lib.nacos.NacosConfig;
import com.dpeng.gateway.config.pojo.RouteDefinition;
import com.dpeng.gateway.config.service.ConfigCenterProcessor;
import com.dpeng.gateway.config.service.RoutesChangeListener;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class NacosConfigCenter implements ConfigCenterProcessor {

    /**
     * 配置
     */
    private ConfigCenter configCenter;

    /**
     * Nacos提供的与配置中心进行交互的接口
     */
    private ConfigService configService;

    /**
     * 是否完成初始化
     */
    private final AtomicBoolean init = new AtomicBoolean(false);


    @SneakyThrows(NacosException.class)
    public void init(ConfigCenter configCenter) {
        //未开启或未完成初始化
        if (!configCenter.isEnabled() || !init.compareAndSet(false, true)) {
            return;
        }
        this.configCenter = configCenter;
        this.configService = NacosFactory.createConfigService(buildProperties(configCenter));
    }

    @SneakyThrows(NacosException.class)
    public void subscribeRoutesChange(RoutesChangeListener listener) {
        if (!configCenter.isEnabled() || !init.get()) {
            return;
        }
        //当服务未初始化且未启动时
        NacosConfig nacos = configCenter.getNacos();
        // 获取路由配置信息
        String configJson = configService.getConfig(nacos.getDataId(), nacos.getGroup(), nacos.getTimeout());
        /* configJson:
         * {
         *     "routes": [
         *         {
         *             "id": "user-service-route",
         *             "serviceName": "user-service",
         *             "uri": "/api/user/**"
         *         }
         *     ]
         * }
         */
        log.info("config from nacos: \n{}", configJson);
        List<RouteDefinition> routes = JSON.parseObject(configJson).getJSONArray("routes").toJavaList(RouteDefinition.class);
        //1.刚初始化时调用一次获取配置并处理
        listener.onRoutesChange(routes);

        //后续配置更改时调用,使用nacos提供的监听功能获取新的配置
        configService.addListener(nacos.getDataId(), nacos.getGroup(), new Listener() {
            @Override
            public Executor getExecutor() {
                return null;
            }

            @Override
            public void receiveConfigInfo(String configInfo) {
                log.info("config change from nacos: {}", configInfo);
                List<RouteDefinition> routes = JSON.parseObject(configInfo).getJSONArray("routes").toJavaList(RouteDefinition.class);
                listener.onRoutesChange(routes);
            }
        });

    }

    private Properties buildProperties(ConfigCenter configCenter) {
        ObjectMapper mapper = new ObjectMapper();
        Properties properties = new Properties();
        properties.put(PropertyKeyConst.SERVER_ADDR, configCenter.getAddress());
        Map map = mapper.convertValue(configCenter.getNacos(), Map.class);
        if (map == null || map.isEmpty()) return properties;
        properties.putAll(map);
        return properties;
    }

}
