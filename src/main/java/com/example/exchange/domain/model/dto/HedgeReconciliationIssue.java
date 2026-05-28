/*
 * 檔案用途：做市商 hedge decision 與 venue fill 對帳差異項目。
 */
package com.example.exchange.domain.model.dto;

import java.math.BigDecimal;

public record HedgeReconciliationIssue(
        String marketMakerId,
        String symbol,
        String refId,
        String venueOrderId,
        String reason,
        BigDecimal expectedNotional,
        BigDecimal filledNotional
) {
    public HedgeReconciliationIssue {
        expectedNotional = expectedNotional == null ? BigDecimal.ZERO : expectedNotional;
        filledNotional = filledNotional == null ? BigDecimal.ZERO : filledNotional;
    }
}
