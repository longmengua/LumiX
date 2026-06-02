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
    /**
     * 流程：啟動 service timer -> 記錄 filled order、cancel、trade events -> 讀 snapshot 驗證各 counter。
     */
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
        assertThat(snapshot.matchingRequests()).isEqualTo(1);
        assertThat(snapshot.matchingFilled()).isEqualTo(1);
        assertThat(snapshot.matchingFillRate()).isEqualTo(1.0);
        assertThat(snapshot.matchingLatencyCount()).isEqualTo(1);
        assertThat(snapshot.matchingLatencyMaxMs()).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("matching 指標會計算拒單率與成交率")
    /**
     * 流程：三筆 matching 嘗試包含 filled、partial fill、rejected；
     * 期望：fill rate 只看有成交量的結果，rejection rate 只看最終拒單結果。
     */
    void recordsMatchingRejectionAndFillRates() {
        OperationalMetricsService service = new OperationalMetricsService();

        service.recordOrderResult(Order.builder().status(Order.Status.FILLED).build(), service.startTimer());
        service.recordOrderResult(Order.builder().status(Order.Status.PARTIALLY_FILLED).build(), service.startTimer());
        service.recordOrderResult(Order.builder().status(Order.Status.REJECTED).build(), service.startTimer());

        var snapshot = service.snapshot();
        assertThat(snapshot.matchingRequests()).isEqualTo(3);
        assertThat(snapshot.matchingFilled()).isEqualTo(2);
        assertThat(snapshot.matchingRejected()).isEqualTo(1);
        assertThat(snapshot.matchingFillRate()).isEqualTo(2.0 / 3.0);
        assertThat(snapshot.matchingRejectionRate()).isEqualTo(1.0 / 3.0);
        assertThat(snapshot.matchingLatencyCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("DB 與 Redis latency collectors 會計算樣本數、平均與最大值")
    /**
     * 流程：直接送入已量測的 DB / Redis 毫秒值；
     * 期望：snapshot 用整數平均值與最大值呈現 production hot path latency baseline。
     */
    void recordsDatabaseAndRedisLatencyMetrics() {
        OperationalMetricsService service = new OperationalMetricsService();

        service.recordDatabaseLatencyMillis(12);
        service.recordDatabaseLatencyMillis(18);
        service.recordRedisLatencyMillis(3);
        service.recordRedisLatencyMillis(9);

        var snapshot = service.snapshot();
        assertThat(snapshot.databaseLatencyCount()).isEqualTo(2);
        assertThat(snapshot.databaseLatencyAvgMs()).isEqualTo(15);
        assertThat(snapshot.databaseLatencyMaxMs()).isEqualTo(18);
        assertThat(snapshot.redisLatencyCount()).isEqualTo(2);
        assertThat(snapshot.redisLatencyAvgMs()).isEqualTo(6);
        assertThat(snapshot.redisLatencyMaxMs()).isEqualTo(9);
    }
}
