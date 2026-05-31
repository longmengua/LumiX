/*
 * 檔案用途：做市商 active quote state 與 open order 對帳差異項目。
 */
package com.example.exchange.domain.model.dto;

import java.util.UUID;

public record MarketMakerQuoteReconciliationIssue(
        String marketMakerId,
        long uid,
        String symbol,
        String side,
        UUID orderId,
        String clientOrderId,
        String reason
) {
}
