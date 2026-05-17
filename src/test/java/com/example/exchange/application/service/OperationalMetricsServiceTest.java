/*
 * 檔案用途：測試輕量營運 metrics counters。
 */
package com.example.exchange.application.service;

import com.example.exchange.domain.model.entity.Order;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 覆蓋 OperationalMetricsService 的 in-memory counter snapshot，
 * 確認下單、撤單、成交事件與延遲資料都被納入營運指標。
 */
class OperationalMetricsServiceTest {

    @Test
    @DisplayName("記錄下單結果、取消數、成交事件數與延遲統計")
    void recordsOrderStatusLatencyCancellationAndTrades() {
        OperationalMetricsService service = new OperationalMetricsService();
        // timer 必須由 service 產生，才能測到 snapshot 的 latency count/max。
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
