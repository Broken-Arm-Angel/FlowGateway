package com.dpeng.gateway.core.filter;

import cn.hutool.core.collection.ConcurrentHashSet;
import com.dpeng.gateway.config.config.manager.DynamicConfigManager;
import com.dpeng.gateway.config.pojo.RouteDefinition;
import com.dpeng.gateway.core.context.GatewayContext;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.dpeng.gateway.common.constant.FilterConstant.*;

/**
 * 过滤器链工厂, 通过网关上下文为对应的服务创建过滤器链, 可同时存在多个服务的过滤器链
 */
@Slf4j
public class FilterChainFactory {
    //所有类型的过滤器对象Map,通过唯一标识获取对应的过滤器对象
    private static final Map<String, Filter> filterMap = new HashMap<>();

    //服务对应的过滤器链,每个服务对应一个过滤器链,过滤器类型分三种: PreFilter, Filter(获取动态配置的过滤器), PostFilter
    private static final Map<String, FilterChain> filterChainMap = new ConcurrentHashMap<>();

    private static final Set<String> addListener = new ConcurrentHashSet<>();

    //类加载时执行, 通过SPI(Service Provider Interface)机制动态加载实现类
    static {
        ServiceLoader<Filter> serviceLoader = ServiceLoader.load(Filter.class);
        for (Filter filter : serviceLoader) {
            filterMap.put(filter.mark(), filter);
            log.info("load filter success: {}", filter);
        }
    }


    public static void buildFilterChain(GatewayContext ctx) {
        String serviceName = ctx.getRoute().getServiceName();
        FilterChain filterChain = filterChainMap.computeIfAbsent(serviceName, name -> {
            FilterChain chain = new FilterChain();
            addPreFilter(chain);
            addFilter(chain, ctx.getRoute().getFilterConfigs());
            addPostFilter(chain);
            chain.sort();
            //监听服务的路由变更
            if (!addListener.contains(serviceName)) {
                //DynamicConfigManager.getInstance().addRouteListener(...) 监听 Nacos / 配置中心的变更。
                DynamicConfigManager.getInstance().addRouteListener(
                        //当某个服务的路由信息变更（例如：过滤器规则改变），就会触发回调函数
                        serviceName, newRoute -> filterChainMap.remove(newRoute.getServiceName())
                );
                addListener.add(serviceName);
            }
            return chain;
        });
        ctx.setFilterChain(filterChain);
    }

    private static void addPreFilter(FilterChain chain) {
        addFilterIfPresent(chain, CORS_FILTER_NAME);
        addFilterIfPresent(chain, FLOW_FILTER_NAME);
        addFilterIfPresent(chain, GRAY_FILTER_NAME);
        addFilterIfPresent(chain, LOAD_BALANCE_FILTER_NAME);
    }

    private static void addFilter(FilterChain chain, Set<RouteDefinition.FilterConfig> filterConfigs) {
        if (filterConfigs == null || filterConfigs.isEmpty()) return;
        for (RouteDefinition.FilterConfig filterConfig : filterConfigs) {
            if (!addFilterIfPresent(chain, filterConfig.getName())) {
                log.info("not found filter: {}", filterConfig.getName());
            }
        }
    }

    private static void addPostFilter(FilterChain chain) {
        addFilterIfPresent(chain, ROUTE_FILTER_NAME);
    }

    private static boolean addFilterIfPresent(FilterChain chain, String filterName) {
        Filter filter = filterMap.get(filterName);
        if (null != filter) {
            chain.add(filter);
            return true;
        }
        return false;
    }


}
