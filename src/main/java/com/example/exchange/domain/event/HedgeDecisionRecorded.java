/*
 * 檔案用途：領域事件，記錄做市商對沖決策與送單結果。
 */
package com.example.exchange.domain.event;

import java.math.BigDecimal;
import java.time.Instant;

public record HedgeDecisionRecorded(
        String marketMakerId,
        String symbol,
        boolean accepted,
        String reason,
        BigDecimal orderNotional,
        String venueOrderId,
        String refId,
        Instant decidedAt
) {
}
