/*
 * 檔案用途：記錄自動修復做市商 quote/open-order 對帳差異時採取的單一步驟。
 */
package com.example.exchange.domain.model.dto;

import java.util.UUID;

public record MarketMakerQuoteRepairAction(
        String marketMakerId,
        long uid,
        String symbol,
        String side,
        UUID orderId,
        String clientOrderId,
        String action,
        String reason,
        boolean success
) {
}
