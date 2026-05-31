/*
 * 檔案用途：做市商 active quote state 與實際 open order 的對帳報告。
 */
package com.example.exchange.domain.model.dto;

import java.time.Instant;
import java.util.List;

public record MarketMakerQuoteReconciliationReport(
        int checkedStates,
        int issueCount,
        Instant generatedAt,
        List<MarketMakerQuoteReconciliationIssue> issues
) {
    public MarketMakerQuoteReconciliationReport {
        generatedAt = generatedAt == null ? Instant.now() : generatedAt;
        issues = issues == null ? List.of() : List.copyOf(issues);
    }
}
