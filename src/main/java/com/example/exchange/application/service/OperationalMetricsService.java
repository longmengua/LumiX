/*
 * 檔案用途：應用服務，提供輕量營運 metrics counters 與延遲統計。
 */
package com.example.exchange.application.service;

import com.example.exchange.domain.model.dto.OperationalMetricsSnapshot;
import com.example.exchange.domain.model.entity.Order;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

@Service
public class OperationalMetricsService {

    // 使用 LongAdder 降低高併發下的 CAS 競爭；這些 counters 是 in-process baseline。
    private final LongAdder orderRequests = new LongAdder();
    private final LongAdder orderNew = new LongAdder();
    private final LongAdder orderPartiallyFilled = new LongAdder();
    private final LongAdder orderFilled = new LongAdder();
    private final LongAdder orderRejected = new LongAdder();
    private final LongAdder orderCanceled = new LongAdder();
    private final LongAdder orderExpired = new LongAdder();
    private final LongAdder tradeEvents = new LongAdder();
    private final LongAdder orderLatencyCount = new LongAdder();
    private final LongAdder orderLatencyTotalMs = new LongAdder();
    private final AtomicLong orderLatencyMaxMs = new AtomicLong();
    private final LongAdder matchingRequests = new LongAdder();
    private final LongAdder matchingRejected = new LongAdder();
    private final LongAdder matchingFilled = new LongAdder();
    private final LongAdder matchingLatencyCount = new LongAdder();
    private final LongAdder matchingLatencyTotalMs = new LongAdder();
    private final AtomicLong matchingLatencyMaxMs = new AtomicLong();
    private final LongAdder databaseLatencyCount = new LongAdder();
    private final LongAdder databaseLatencyTotalMs = new LongAdder();
    private final AtomicLong databaseLatencyMaxMs = new AtomicLong();
    private final LongAdder redisLatencyCount = new LongAdder();
    private final LongAdder redisLatencyTotalMs = new LongAdder();
    private final AtomicLong redisLatencyMaxMs = new AtomicLong();

    /** 回傳 monotonic timer 起點，呼叫方應在同一流程完成後交給 recordOrderResult。 */
    public long startTimer() {
        return System.nanoTime();
    }

    /** 記錄單筆下單流程的最終狀態與端到端延遲。 */
    public void recordOrderResult(Order order, long startedAtNanos) {
        if (order == null) {
            return;
        }
        orderRequests.increment();
        recordStatus(order.getStatus());
        recordOrderLatency(startedAtNanos);
        recordMatchingResult(order.getStatus(), startedAtNanos);
    }

    /** 批量撤單或 cancel-on-disconnect 會一次增加多筆取消計數。 */
    public void recordCanceledOrders(int count) {
        if (count <= 0) {
            return;
        }
        orderCanceled.add(count);
    }

    /** tradeEvents 記錄 domain trade event 數量，不代表唯一 match 數。 */
    public void recordTradeEvents(int count) {
        if (count <= 0) {
            return;
        }
        tradeEvents.add(count);
    }

    /** 記錄單次 DB operation 延遲；呼叫方可用 startTimer() 建立 startedAtNanos。 */
    public void recordDatabaseLatency(long startedAtNanos) {
        if (startedAtNanos <= 0) {
            return;
        }
        recordDatabaseLatencyMillis(elapsedMs(startedAtNanos));
    }

    /** 記錄已由 adapter 或測試計算好的 DB operation 延遲。 */
    public void recordDatabaseLatencyMillis(long elapsedMs) {
        recordLatency(databaseLatencyCount, databaseLatencyTotalMs, databaseLatencyMaxMs, elapsedMs);
    }

    /** 記錄單次 Redis operation 延遲；呼叫方可用 startTimer() 建立 startedAtNanos。 */
    public void recordRedisLatency(long startedAtNanos) {
        if (startedAtNanos <= 0) {
            return;
        }
        recordRedisLatencyMillis(elapsedMs(startedAtNanos));
    }

    /** 記錄已由 adapter 或測試計算好的 Redis operation 延遲。 */
    public void recordRedisLatencyMillis(long elapsedMs) {
        recordLatency(redisLatencyCount, redisLatencyTotalMs, redisLatencyMaxMs, elapsedMs);
    }

    /** 建立即時快照；不會重置 counters。 */
    public OperationalMetricsSnapshot snapshot() {
        long latencyCount = orderLatencyCount.sum();
        long latencyTotal = orderLatencyTotalMs.sum();
        long matchingRequestCount = matchingRequests.sum();
        long matchingRejectCount = matchingRejected.sum();
        long matchingFillCount = matchingFilled.sum();
        long matchingLatencySamples = matchingLatencyCount.sum();
        long matchingLatencyTotal = matchingLatencyTotalMs.sum();
        long databaseLatencySamples = databaseLatencyCount.sum();
        long databaseLatencyTotal = databaseLatencyTotalMs.sum();
        long redisLatencySamples = redisLatencyCount.sum();
        long redisLatencyTotal = redisLatencyTotalMs.sum();
        return new OperationalMetricsSnapshot(
                orderRequests.sum(),
                orderNew.sum(),
                orderPartiallyFilled.sum(),
                orderFilled.sum(),
                orderRejected.sum(),
                orderCanceled.sum(),
                orderExpired.sum(),
                tradeEvents.sum(),
                latencyCount,
                latencyCount == 0 ? 0 : latencyTotal / latencyCount,
                orderLatencyMaxMs.get(),
                matchingRequestCount,
                matchingRejectCount,
                matchingFillCount,
                rate(matchingRejectCount, matchingRequestCount),
                rate(matchingFillCount, matchingRequestCount),
                matchingLatencySamples,
                matchingLatencySamples == 0 ? 0 : matchingLatencyTotal / matchingLatencySamples,
                matchingLatencyMaxMs.get(),
                databaseLatencySamples,
                databaseLatencySamples == 0 ? 0 : databaseLatencyTotal / databaseLatencySamples,
                databaseLatencyMaxMs.get(),
                redisLatencySamples,
                redisLatencySamples == 0 ? 0 : redisLatencyTotal / redisLatencySamples,
                redisLatencyMaxMs.get()
        );
    }

    private void recordStatus(Order.Status status) {
        if (status == null) return;
        switch (status) {
            case NEW -> orderNew.increment();
            case PARTIALLY_FILLED -> orderPartiallyFilled.increment();
            case FILLED -> orderFilled.increment();
            case CANCELED -> orderCanceled.increment();
            case REJECTED -> orderRejected.increment();
            case EXPIRED -> orderExpired.increment();
        }
    }

    private void recordOrderLatency(long startedAtNanos) {
        if (startedAtNanos <= 0) return;
        long elapsedMs = elapsedMs(startedAtNanos);
        orderLatencyCount.increment();
        orderLatencyTotalMs.add(elapsedMs);
        orderLatencyMaxMs.accumulateAndGet(elapsedMs, Math::max);
    }

    private void recordMatchingResult(Order.Status status, long startedAtNanos) {
        if (status == null) return;
        matchingRequests.increment();
        if (status == Order.Status.REJECTED) {
            matchingRejected.increment();
        }
        if (status == Order.Status.PARTIALLY_FILLED || status == Order.Status.FILLED) {
            matchingFilled.increment();
        }
        recordMatchingLatency(startedAtNanos);
    }

    private void recordMatchingLatency(long startedAtNanos) {
        if (startedAtNanos <= 0) return;
        long elapsedMs = elapsedMs(startedAtNanos);
        matchingLatencyCount.increment();
        matchingLatencyTotalMs.add(elapsedMs);
        matchingLatencyMaxMs.accumulateAndGet(elapsedMs, Math::max);
    }

    private static long elapsedMs(long startedAtNanos) {
        return Math.max(0, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAtNanos));
    }

    private static void recordLatency(
            LongAdder count,
            LongAdder totalMs,
            AtomicLong maxMs,
            long elapsedMs
    ) {
        if (elapsedMs < 0) {
            return;
        }
        count.increment();
        totalMs.add(elapsedMs);
        maxMs.accumulateAndGet(elapsedMs, Math::max);
    }

    private static double rate(long numerator, long denominator) {
        if (denominator <= 0) {
            return 0.0;
        }
        return (double) numerator / denominator;
    }
}
