/*
 * 檔案用途：領域事件，記錄做市商 quote command 是否通過風控。
 */
package com.example.exchange.domain.event;

import java.time.Instant;

public record MarketMakerQuoteDecisionRecorded(
        String marketMakerId,
        String symbol,
        boolean accepted,
        String reason,
        String refId,
        Instant decidedAt
) {
}
