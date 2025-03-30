package com.dpeng.gateway.core.algorithm;

import com.dpeng.gateway.common.enums.ResponseCode;
import com.dpeng.gateway.common.exception.LimitedException;
import com.dpeng.gateway.core.context.GatewayContext;
import com.dpeng.gateway.core.filter.flow.RateLimiter;
import io.netty.channel.EventLoopGroup;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 限流: 漏桶算法;需要严格控制流量的情况，保证请求以恒定速率进入后端。
 */
public class LeakyBucketRateLimiter implements RateLimiter {

    private final int bucketCapacity; // 漏桶的容量
    private final long leakInterval;  // 漏水的时间间隔
    private final AtomicInteger currentWaterLevel; // 当前桶中的水量
    private final Queue<GatewayContext> waitingQueue; // 等待队列

    public LeakyBucketRateLimiter(int capacity, long leakInterval, EventLoopGroup eventLoopGroup) {
        this.bucketCapacity = capacity;
        this.leakInterval = leakInterval;
        this.currentWaterLevel = new AtomicInteger(0);
        this.waitingQueue = new ConcurrentLinkedQueue<>();
        // 定时漏水的任务
        startLeakTask(eventLoopGroup);
    }

    //请求会以恒定的速度处理，不会因为流量突增导致系统崩溃;即使短时间有大量请求进入，也会排队等待处理。
    private void startLeakTask(EventLoopGroup eventLoopGroup) {
        // 使用 Netty 的定时任务来按频率漏出请求
        eventLoopGroup.scheduleAtFixedRate(() -> {
            if (!waitingQueue.isEmpty() && currentWaterLevel.get() > 0) {
                GatewayContext gatewayContext = waitingQueue.poll();
                if (gatewayContext != null) {
                    // 重新提交请求到 Netty 事件循环
                    gatewayContext.getNettyCtx().executor().execute(() -> {
                        currentWaterLevel.decrementAndGet();
                        gatewayContext.doFilter();
                    });
                }
            }
        }, leakInterval, leakInterval, TimeUnit.MILLISECONDS);
    }

    //请求不会立即执行，而是排队，等待 startLeakTask 逐个处理。
    @Override
    public void tryConsume(GatewayContext context) {
        if (currentWaterLevel.get() < bucketCapacity) {
            // 如果桶未满，将请求加入等待队列
            currentWaterLevel.incrementAndGet();
            waitingQueue.offer(context);
        } else {
            // 如果桶满，直接拒绝请求
            throw new LimitedException(ResponseCode.TOO_MANY_REQUESTS);
        }
    }

}
