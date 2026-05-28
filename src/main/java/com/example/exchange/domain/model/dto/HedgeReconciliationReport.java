/*
 * 檔案用途：做市商 hedge reconciliation 報告。
 */
package com.example.exchange.domain.model.dto;

import java.time.Instant;
import java.util.List;

public record HedgeReconciliationReport(
        String marketMakerId,
        int checkedDecisions,
        int issueCount,
        Instant generatedAt,
        List<HedgeReconciliationIssue> issues
) {
    public HedgeReconciliationReport {
        generatedAt = generatedAt == null ? Instant.now() : generatedAt;
        issues = issues == null ? List.of() : List.copyOf(issues);
    }
}
