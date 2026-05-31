/*
 * 檔案用途：領域事件，描述訂單從建立、接受、拒絕、成交、過期到取消的生命週期變更。
 */
package com.example.exchange.domain.event;

import com.example.exchange.domain.model.entity.Order;
import com.example.exchange.domain.model.entity.Symbol;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * 訂單生命週期事件。
 * - 用於審計、推送、對帳與日後重建 order state projection。
 * - 事件保留訂單當下快照，避免 consumer 再回查 mutable order state。
 */
public record OrderLifecycleEvent(
        UUID orderId,
        long uid,
        Symbol symbol,
        String clientOrderId,
        String strategyId,
        String marketMakerId,
        Stage stage,
        Order.Status status,
        String reasonCode,
        BigDecimal price,
        BigDecimal origQty,
        BigDecimal remainingQty,
        BigDecimal executedQty,
        BigDecimal avgPrice,
        Instant ts
) {
    public enum Stage {
        CREATED,
        ACCEPTED,
        UPDATED,
        REJECTED,
        CANCELED,
        EXPIRED,
        FILLED
    }

    public static OrderLifecycleEvent created(Order order) {
        return from(order, Stage.CREATED, null);
    }

    public static OrderLifecycleEvent accepted(Order order) {
        return from(order, Stage.ACCEPTED, null);
    }

    public static OrderLifecycleEvent updated(Order order) {
        Stage stage = switch (order.getStatus()) {
            case FILLED -> Stage.FILLED;
            case CANCELED -> Stage.CANCELED;
            case REJECTED -> Stage.REJECTED;
            case EXPIRED -> Stage.EXPIRED;
            default -> Stage.UPDATED;
        };
        return from(order, stage, order.getRejectCode());
    }

    public static OrderLifecycleEvent rejected(Order order) {
        return from(order, Stage.REJECTED, order.getRejectCode());
    }

    public static OrderLifecycleEvent canceled(Order order) {
        return from(order, Stage.CANCELED, null);
    }

    private static OrderLifecycleEvent from(Order order, Stage stage, String reasonCode) {
        return new OrderLifecycleEvent(
                order.getId(),
                order.getUid(),
                order.getSymbol(),
                order.getClientOrderId(),
                order.getStrategyId(),
                order.getMarketMakerId(),
                stage,
                order.getStatus(),
                reasonCode,
                order.getPrice(),
                order.getOrigQty(),
                order.getQty(),
                order.getExecutedQty(),
                order.getAvgPrice(),
                Instant.now()
        );
    }
}
