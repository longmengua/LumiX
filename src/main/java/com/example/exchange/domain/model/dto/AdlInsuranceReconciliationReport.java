/*
 * 檔案用途：ADL queue、liquidated position coverage 與 insurance fund balance 的 reconciliation report。
 */
package com.example.exchange.domain.model.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record AdlInsuranceReconciliationReport(
        String asset,
        BigDecimal insuranceFundBalance,
        int openAdlQueueCount,
        BigDecimal openAdlQueueAmount,
        int issueCount,
        Instant generatedAt,
        List<AdlInsuranceReconciliationIssue> issues
) {
    public AdlInsuranceReconciliationReport {
        insuranceFundBalance = insuranceFundBalance == null ? BigDecimal.ZERO : insuranceFundBalance;
        openAdlQueueAmount = openAdlQueueAmount == null ? BigDecimal.ZERO : openAdlQueueAmount;
        issues = issues == null ? List.of() : List.copyOf(issues);
    }
}
