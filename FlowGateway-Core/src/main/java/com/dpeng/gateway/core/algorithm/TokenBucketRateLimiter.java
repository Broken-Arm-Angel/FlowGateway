package com.dpeng.gateway.core.algorithm;


import com.dpeng.gateway.common.enums.ResponseCode;
import com.dpeng.gateway.common.exception.LimitedException;
import com.dpeng.gateway.core.context.GatewayContext;
import com.dpeng.gateway.core.filter.flow.RateLimiter;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 限流: 令牌桶算法;适用场景：突发流量的情况（能短时间内允许一些突发请求）。
 */
public class TokenBucketRateLimiter implements RateLimiter {

    private final int capacity; // 桶的容量
    private final int refillRate; // 令牌生成速率
    private final AtomicInteger tokens; // 当前令牌数量

    public TokenBucketRateLimiter(int capacity, int refillRatePerSecond) {
        this.capacity = capacity;
        this.refillRate = refillRatePerSecond;
        this.tokens = new AtomicInteger(0);//tokens 初始值为 0（刚创建时桶是空的）
        startRefilling();//调用 startRefilling() 开始定期补充令牌。
    }

    @Override
    public void tryConsume(GatewayContext context) {
        //先获取当前令牌数并减少 1。
        if (tokens.getAndDecrement() > 0) {
            //如果桶里有令牌（>0），则允许请求通过，执行 context.doFilter() 继续请求处理。
            context.doFilter();
        } else {
            //如果没有令牌（<=0），则立即拒绝请求, 并回复为0
            tokens.incrementAndGet();
            throw new LimitedException(ResponseCode.TOO_MANY_REQUESTS);
        }
    }

    private void startRefilling() {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(this::refillTokens, 0, 1000, TimeUnit.MILLISECONDS);
    }

    private void refillTokens() {
        //在当前令牌数基础上增加 refillRate 个令牌, 确保令牌数不超过桶的最大容量（capacity）
        tokens.set(Math.min(capacity, tokens.get() + refillRate));
    }

}
