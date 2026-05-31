/*
 * 檔案用途：Web DTO，對外回傳 order lifecycle 最新狀態 projection。
 */
package com.example.exchange.interfaces.web.dto;

import com.example.exchange.domain.model.entity.OrderLifecycleProjection;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderLifecycleProjectionResponse(
        String orderId,
        Integer schemaVersion,
        Long uid,
        String symbol,
        String clientOrderId,
        String strategyId,
        String marketMakerId,
        String latestStage,
        String status,
        String reasonCode,
        BigDecimal price,
        BigDecimal origQty,
        BigDecimal remainingQty,
        BigDecimal executedQty,
        BigDecimal avgPrice,
        Instant firstEventAt,
        Instant lastEventAt,
        Instant updatedAt
) {
    public static OrderLifecycleProjectionResponse from(OrderLifecycleProjection projection) {
        return new OrderLifecycleProjectionResponse(
                projection.getOrderId(),
                projection.getSchemaVersion(),
                projection.getUid(),
                projection.getSymbol(),
                projection.getClientOrderId(),
                projection.getStrategyId(),
                projection.getMarketMakerId(),
                projection.getLatestStage(),
                projection.getStatus(),
                projection.getReasonCode(),
                projection.getPrice(),
                projection.getOrigQty(),
                projection.getRemainingQty(),
                projection.getExecutedQty(),
                projection.getAvgPrice(),
                projection.getFirstEventAt(),
                projection.getLastEventAt(),
                projection.getUpdatedAt()
        );
    }
}
