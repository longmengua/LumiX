package com.lumix.trading.core.spot.orderbook;

import com.lumix.trading.core.spot.orderintake.SpotOrderSide;
import com.lumix.trading.core.spot.orderintake.SpotOrderType;
import com.lumix.trading.core.spot.orderintake.SpotTimeInForce;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Spot sandbox order record。
 *
 * 這份 record 只存在於 in-memory book，不代表已持久化、已 reservation、已 matching 或已 settlement。
 */
public record SpotSandboxOrderRecord(
        String sandboxOrderId,
        String requestId,
        String idempotencyKey,
        String userId,
        String accountId,
        String marketSymbol,
        SpotOrderSide side,
        SpotOrderType type,
        BigDecimal price,
        BigDecimal quantity,
        BigDecimal remainingQuantity,
        SpotTimeInForce timeInForce,
        SpotSandboxOrderStatus status,
        Instant acceptedAt
) {
}
