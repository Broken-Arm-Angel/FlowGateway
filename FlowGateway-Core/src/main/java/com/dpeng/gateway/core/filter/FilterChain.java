package com.dpeng.gateway.core.filter;

import com.dpeng.gateway.core.context.GatewayContext;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
public class FilterChain {

    private final List<Filter> filters = new ArrayList<>();

    public FilterChain add(Filter filter) {
        filters.add(filter);
        return this;
    }

    public FilterChain add(List<Filter> filter) {
        filters.addAll(filter);
        return this;
    }

    public void sort() {
        filters.sort(Comparator.comparingInt(Filter::getOrder));
    }

    public int size() {
        return filters.size();
    }

    /**
     * 执行前置过滤器
     */
    public void doPreFilter(int index, GatewayContext context) {
        if (index < filters.size() && index >= 0) {
            filters.get(index).doPreFilter(context);
        }
    }

    /**
     * 执行后置过滤器
     */
    public void doPostFilter(int index, GatewayContext context) {
        if (index < filters.size() && index >= 0) {
            filters.get(index).doPostFilter(context);
        }
    }


}
