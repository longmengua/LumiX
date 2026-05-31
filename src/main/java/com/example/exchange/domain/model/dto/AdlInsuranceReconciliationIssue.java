/*
 * 檔案用途：ADL/insurance-fund shortfall reconciliation 的單筆差異。
 */
package com.example.exchange.domain.model.dto;

import java.math.BigDecimal;

public record AdlInsuranceReconciliationIssue(
        String liquidationId,
        long uid,
        String symbol,
        String reason,
        BigDecimal queueAmount,
        BigDecimal positionAdlCovered,
        BigDecimal positionInsuranceCovered,
        String queueOwner
) {
    public AdlInsuranceReconciliationIssue {
        queueAmount = queueAmount == null ? BigDecimal.ZERO : queueAmount;
        positionAdlCovered = positionAdlCovered == null ? BigDecimal.ZERO : positionAdlCovered;
        positionInsuranceCovered = positionInsuranceCovered == null ? BigDecimal.ZERO : positionInsuranceCovered;
    }
}
