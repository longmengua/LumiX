/*
 * 檔案用途：做市商 quote command 的風控決策結果。
 */
package com.example.exchange.domain.model.dto;

import java.time.Instant;

public record MarketMakerQuoteDecision(
        String marketMakerId,
        String symbol,
        boolean accepted,
        String reason,
        Instant decidedAt
) {
}
