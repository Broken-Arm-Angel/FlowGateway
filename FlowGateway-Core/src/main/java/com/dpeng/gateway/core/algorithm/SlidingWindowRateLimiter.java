package com.dpeng.gateway.core.algorithm;


import com.dpeng.gateway.common.enums.ResponseCode;
import com.dpeng.gateway.common.exception.LimitedException;
import com.dpeng.gateway.core.context.GatewayContext;
import com.dpeng.gateway.core.filter.flow.RateLimiter;

import java.time.Instant;
import java.util.Deque;
import java.util.LinkedList;

/**
 * 限流: 滑动窗口;适用于统计类限流
 */
public class SlidingWindowRateLimiter implements RateLimiter {

    private final int capacity; // 最大允许请求数
    private final int windowSizeInMillis; // 窗口大小，单位：毫秒
    private final Deque<Long> requestTimestamps; // 存储每个请求的时间戳

    public SlidingWindowRateLimiter(int capacity, int windowSizeInMillis) {
        this.capacity = capacity;
        this.windowSizeInMillis = windowSizeInMillis;
        this.requestTimestamps = new LinkedList<>();
    }

    @Override
    public synchronized void tryConsume(GatewayContext context) {
        //先 获取当前时间。
        long now = Instant.now().toEpochMilli();
        //清除当前时间为止的前一秒(滑动窗口大小内)的所有请求, 因为只需要判断滑动窗口内的所有请求是否小于最大请求数,小于则放行, 否则返回异常
        cleanOldRequests(now);
        //判断当前窗口内请求数是否超限
        if (requestTimestamps.size() < capacity) {
            //未超限：记录当前请求时间并继续执行 doFilter() 处理请求。
            requestTimestamps.addLast(now);
            context.doFilter();
        } else {
            //超限：抛出 LimitedException。
            throw new LimitedException(ResponseCode.TOO_MANY_REQUESTS);
        }
    }

    private void cleanOldRequests(long currentTime) {
        while (!requestTimestamps.isEmpty() && (currentTime - requestTimestamps.peekFirst()) > windowSizeInMillis) {
            requestTimestamps.pollFirst();
        }
    }

}
