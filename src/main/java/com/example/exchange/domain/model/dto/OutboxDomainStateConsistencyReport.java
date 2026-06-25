/*
 * 檔案用途：outbox row 與 domain-state transition 對齊檢查報告。
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
public class OutboxDomainStateConsistencyReport {

    private final int scanned;

    private final int issueCount;

    private final Instant generatedAt;

    private final List<OutboxDomainStateConsistencyIssue> issues;
    public OutboxDomainStateConsistencyReport(int scanned, int issueCount, Instant generatedAt, List<OutboxDomainStateConsistencyIssue> issues) {
        generatedAt = generatedAt == null ? Instant.now() : generatedAt;
        issues = issues == null ? List.of() : List.copyOf(issues);
    
        this.scanned = scanned;
        this.issueCount = issueCount;
        this.generatedAt = generatedAt;
        this.issues = issues;
    }

    public int scanned() {
        return scanned;
    }

    public int issueCount() {
        return issueCount;
    }

    public Instant generatedAt() {
        return generatedAt;
    }

    public List<OutboxDomainStateConsistencyIssue> issues() {
        return issues;
    }
}