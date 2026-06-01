/*
 * 檔案用途：做市商 quote reconciliation repair 的執行結果。
 */
package com.example.exchange.domain.model.dto;

import java.time.Instant;
import java.util.List;

public record MarketMakerQuoteRepairReport(
        int checkedStates,
        int issueCount,
        int canceledOrders,
        int deactivatedStates,
        Instant generatedAt,
        List<MarketMakerQuoteRepairAction> actions
) {
    public MarketMakerQuoteRepairReport {
        generatedAt = generatedAt == null ? Instant.now() : generatedAt;
        actions = actions == null ? List.of() : List.copyOf(actions);
    }
}
