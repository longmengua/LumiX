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

    public long startTimer() {
        return System.nanoTime();
    }

    public void recordOrderResult(Order order, long startedAtNanos) {
        if (order == null) {
            return;
        }
        orderRequests.increment();
        recordStatus(order.getStatus());
        recordOrderLatency(startedAtNanos);
    }

    public void recordCanceledOrders(int count) {
        if (count <= 0) {
            return;
        }
        orderCanceled.add(count);
    }

    public void recordTradeEvents(int count) {
        if (count <= 0) {
            return;
        }
        tradeEvents.add(count);
    }

    public OperationalMetricsSnapshot snapshot() {
        long latencyCount = orderLatencyCount.sum();
        long latencyTotal = orderLatencyTotalMs.sum();
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
                orderLatencyMaxMs.get()
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
        long elapsedMs = Math.max(0, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAtNanos));
        orderLatencyCount.increment();
        orderLatencyTotalMs.add(elapsedMs);
        orderLatencyMaxMs.accumulateAndGet(elapsedMs, Math::max);
    }
}
