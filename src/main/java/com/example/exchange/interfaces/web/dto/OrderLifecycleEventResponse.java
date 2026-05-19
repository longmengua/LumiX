/*
 * 檔案用途：Web DTO，對外回傳 order lifecycle durable event log。
 */
package com.example.exchange.interfaces.web.dto;

import com.example.exchange.domain.model.entity.OrderLifecycleEventRecord;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderLifecycleEventResponse(
        Long id,
        Integer schemaVersion,
        String orderId,
        Long uid,
        String symbol,
        String clientOrderId,
        String stage,
        String status,
        String reasonCode,
        BigDecimal price,
        BigDecimal origQty,
        BigDecimal remainingQty,
        BigDecimal executedQty,
        BigDecimal avgPrice,
        Instant eventTs,
        Instant recordedAt
) {
    public static OrderLifecycleEventResponse from(OrderLifecycleEventRecord record) {
        return new OrderLifecycleEventResponse(
                record.getId(),
                record.getSchemaVersion(),
                record.getOrderId(),
                record.getUid(),
                record.getSymbol(),
                record.getClientOrderId(),
                record.getStage(),
                record.getStatus(),
                record.getReasonCode(),
                record.getPrice(),
                record.getOrigQty(),
                record.getRemainingQty(),
                record.getExecutedQty(),
                record.getAvgPrice(),
                record.getEventTs(),
                record.getRecordedAt()
        );
    }
}
