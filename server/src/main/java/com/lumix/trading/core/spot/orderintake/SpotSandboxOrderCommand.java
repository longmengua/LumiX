package com.lumix.trading.core.spot.orderintake;

import java.math.BigDecimal;

/**
 * Spot sandbox order intake command。
 *
 * 這份 command 只用於 sandbox-only intake 驗證，不代表已持久化、已 reservation、已 matching 或已 settlement。
 */
public record SpotSandboxOrderCommand(
        String requestId,
        String idempotencyKey,
        String userId,
        String accountId,
        String marketSymbol,
        SpotOrderSide side,
        SpotOrderType type,
        BigDecimal price,
        BigDecimal quantity,
        SpotTimeInForce timeInForce
) {
}
