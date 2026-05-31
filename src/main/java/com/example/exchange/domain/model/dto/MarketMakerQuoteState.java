/*
 * 檔案用途：做市商每個 symbol 的最新 quote ownership 狀態，用於重啟後查回 active quote orders。
 */
package com.example.exchange.domain.model.dto;

import java.time.Instant;
import java.util.UUID;

public record MarketMakerQuoteState(
        String marketMakerId,
        long uid,
        String symbol,
        String refId,
        boolean active,
        boolean accepted,
        String reason,
        int canceledCount,
        UUID bidOrderId,
        UUID askOrderId,
        long bidVersion,
        long askVersion,
        UUID replacedBidOrderId,
        UUID replacedAskOrderId,
        Instant updatedAt
) {
}
