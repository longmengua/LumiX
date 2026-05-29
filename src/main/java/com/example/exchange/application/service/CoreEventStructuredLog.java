/*
 * 檔案用途：產生可搜尋的 core-event 結構化日誌欄位，供營運按 uid/orderId/clientOrderId/symbol 查找。
 */
package com.example.exchange.application.service;

import com.example.exchange.domain.model.entity.OrderLifecycleEventRecord;

import java.time.Instant;

public final class CoreEventStructuredLog {

    private CoreEventStructuredLog() {
    }

    public static String orderLifecycle(OrderLifecycleEventRecord record) {
        if (record == null) {
            return "CORE_EVENT eventType=ORDER_LIFECYCLE";
        }
        return "CORE_EVENT"
                + " eventType=ORDER_LIFECYCLE"
                + " uid=" + record.getUid()
                + " orderId=" + value(record.getOrderId())
                + " clientOrderId=" + value(record.getClientOrderId())
                + " symbol=" + value(record.getSymbol())
                + " stage=" + value(record.getStage())
                + " status=" + value(record.getStatus())
                + " reasonCode=" + value(record.getReasonCode())
                + " eventTs=" + value(record.getEventTs());
    }

    private static String value(Object value) {
        if (value == null) return "-";
        String text = value instanceof Instant instant ? instant.toString() : value.toString();
        if (text.isBlank()) return "-";
        return text.trim().replaceAll("\\s+", "_");
    }
}
