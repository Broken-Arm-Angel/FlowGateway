package com.dpeng.gateway.config.config.manager;

import com.dpeng.gateway.config.pojo.RouteDefinition;

public interface RouteListener {

    void changeOnRoute(RouteDefinition routeDefinition);

}
