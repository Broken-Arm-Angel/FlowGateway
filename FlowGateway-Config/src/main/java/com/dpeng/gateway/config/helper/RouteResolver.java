package com.dpeng.gateway.config.helper;

import com.dpeng.gateway.common.enums.ResponseCode;
import com.dpeng.gateway.common.exception.NotFoundException;
import com.dpeng.gateway.config.config.manager.DynamicConfigManager;
import com.dpeng.gateway.config.pojo.RouteDefinition;

import java.util.*;
import java.util.regex.Pattern;


/**
 * 根据 请求的 uri，在 路由表 中找到 匹配的 RouteDefinition，并返回优先级最高的那一个。
 */
public class RouteResolver {

    private static final DynamicConfigManager manager = DynamicConfigManager.getInstance();

    /**
     * 根据uri解析出对应的路由
     */
    public static RouteDefinition matchingRouteByUri(String uri) {
        // 1. 获取所有的路由配置（URI -> RouteDefinition）
        Set<Map.Entry<String, RouteDefinition>> allUriEntry = manager.getAllUriEntry();

        // 2. 存储所有匹配的路由
        List<RouteDefinition> matchedRoute = new ArrayList<>();

        // 3. 遍历所有路由，使用正则匹配请求的 URI
        for (Map.Entry<String, RouteDefinition> entry: allUriEntry) {
            String regex = entry.getKey().replace("**", ".*");// ** 通配符转换为正则
            if (Pattern.matches(regex, uri)) {
                matchedRoute.add(entry.getValue());
            }
        }
        // 4. 如果没有匹配的路由，抛出 404 异常
        if (matchedRoute.isEmpty()) {
            throw new NotFoundException(ResponseCode.PATH_NO_MATCHED);
        }

        // 5. 按 `order` 进行排序（order 值越小，优先级越高）
        matchedRoute.sort(Comparator.comparingInt(RouteDefinition::getOrder));

        // 6. 选出优先级最高的路由：
        //    ① `order` 值最小
        //    ② 若 order 相同，则 URI 长度最长（更具体的路由）
        return matchedRoute.stream()
                .min(Comparator.comparingInt(RouteDefinition::getOrder)
                        .thenComparing(route -> route.getUri().length(), Comparator.reverseOrder()))
                .orElseThrow();
    }

}
