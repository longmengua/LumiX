/*
 * 檔案用途：做市商 quote lifecycle 執行結果，連結 quote decision 與實際內部訂單。
 */
package com.example.exchange.domain.model.dto;

import java.util.UUID;

public record MarketMakerQuoteLifecycleReport(
        MarketMakerQuoteDecision decision,
        int placedCount,
        UUID bidOrderId,
        UUID askOrderId
) {
}
