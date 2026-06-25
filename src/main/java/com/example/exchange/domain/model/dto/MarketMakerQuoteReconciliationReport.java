/*
 * 檔案用途：做市商 active quote state 與實際 open order 的對帳報告。
 */
package com.example.exchange.domain.model.dto;

import java.time.Instant;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;


@Data
@Builder
@Jacksonized
public class MarketMakerQuoteReconciliationReport {

    private final int checkedStates;

    private final int issueCount;

    private final Instant generatedAt;

    private final List<MarketMakerQuoteReconciliationIssue> issues;
    public MarketMakerQuoteReconciliationReport(int checkedStates, int issueCount, Instant generatedAt, List<MarketMakerQuoteReconciliationIssue> issues) {
        generatedAt = generatedAt == null ? Instant.now() : generatedAt;
        issues = issues == null ? List.of() : List.copyOf(issues);
    
        this.checkedStates = checkedStates;
        this.issueCount = issueCount;
        this.generatedAt = generatedAt;
        this.issues = issues;
    }

    public int checkedStates() {
        return checkedStates;
    }

    public int issueCount() {
        return issueCount;
    }

    public Instant generatedAt() {
        return generatedAt;
    }

    public List<MarketMakerQuoteReconciliationIssue> issues() {
        return issues;
    }
}