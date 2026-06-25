/*
 * 檔案用途：做市商 hedge reconciliation 報告。
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
public class HedgeReconciliationReport {

    private final String marketMakerId;

    private final int checkedDecisions;

    private final int issueCount;

    private final Instant generatedAt;

    private final List<HedgeReconciliationIssue> issues;
    public HedgeReconciliationReport(String marketMakerId, int checkedDecisions, int issueCount, Instant generatedAt, List<HedgeReconciliationIssue> issues) {
        generatedAt = generatedAt == null ? Instant.now() : generatedAt;
        issues = issues == null ? List.of() : List.copyOf(issues);
    
        this.marketMakerId = marketMakerId;
        this.checkedDecisions = checkedDecisions;
        this.issueCount = issueCount;
        this.generatedAt = generatedAt;
        this.issues = issues;
    }

    public String marketMakerId() {
        return marketMakerId;
    }

    public int checkedDecisions() {
        return checkedDecisions;
    }

    public int issueCount() {
        return issueCount;
    }

    public Instant generatedAt() {
        return generatedAt;
    }

    public List<HedgeReconciliationIssue> issues() {
        return issues;
    }
}