/*
 * 檔案用途：測試輕量營運 metrics counters。
 */
package com.example.exchange.application.service;

import com.example.exchange.domain.model.entity.Order;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OperationalMetricsServiceTest {

    @Test
    void recordsOrderStatusLatencyCancellationAndTrades() {
        OperationalMetricsService service = new OperationalMetricsService();
        long startedAt = service.startTimer();

        service.recordOrderResult(Order.builder().status(Order.Status.FILLED).build(), startedAt);
        service.recordCanceledOrders(2);
        service.recordTradeEvents(4);

        var snapshot = service.snapshot();
        assertThat(snapshot.orderRequests()).isEqualTo(1);
        assertThat(snapshot.orderFilled()).isEqualTo(1);
        assertThat(snapshot.orderCanceled()).isEqualTo(2);
        assertThat(snapshot.tradeEvents()).isEqualTo(4);
        assertThat(snapshot.orderLatencyCount()).isEqualTo(1);
        assertThat(snapshot.orderLatencyMaxMs()).isGreaterThanOrEqualTo(0);
    }
}
