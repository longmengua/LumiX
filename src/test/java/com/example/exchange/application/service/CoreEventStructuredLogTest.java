/*
 * 檔案用途：測試 core-event structured log formatter，確保營運搜尋欄位穩定。
 */
package com.example.exchange.application.service;

import com.example.exchange.domain.model.entity.OrderLifecycleEventRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class CoreEventStructuredLogTest {

    @Test
    @DisplayName("orderLifecycle log line 包含 uid、orderId、clientOrderId 與 symbol 搜尋欄位")
    void orderLifecycleIncludesSearchFields() {
        OrderLifecycleEventRecord record = new OrderLifecycleEventRecord();
        record.setUid(42L);
        record.setOrderId("order-1");
        record.setClientOrderId("client 1");
        record.setSymbol("BTCUSDT");
        record.setStage("ACCEPTED");
        record.setStatus("NEW");
        record.setReasonCode(null);
        record.setEventTs(Instant.parse("2026-05-30T00:00:00Z"));

        // 營運日誌要能用固定 key 搜尋，含空白的 clientOrderId 會被正規化成單一 token。
        String line = CoreEventStructuredLog.orderLifecycle(record);

        assertThat(line)
                .contains("CORE_EVENT")
                .contains("eventType=ORDER_LIFECYCLE")
                .contains("uid=42")
                .contains("orderId=order-1")
                .contains("clientOrderId=client_1")
                .contains("symbol=BTCUSDT")
                .contains("stage=ACCEPTED")
                .contains("status=NEW")
                .contains("reasonCode=-")
                .contains("eventTs=2026-05-30T00:00:00Z");
    }
}
