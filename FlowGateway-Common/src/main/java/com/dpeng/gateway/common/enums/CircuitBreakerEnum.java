package com.dpeng.gateway.common.enums;

/**
 * 滑动窗口类型
 */
public enum CircuitBreakerEnum {

    /**
     * 基于请求次数的窗口。
     */
    COUNT_BASED,
    /**
     * 基于时间的窗口
     */
    TIME_BASED;

}
