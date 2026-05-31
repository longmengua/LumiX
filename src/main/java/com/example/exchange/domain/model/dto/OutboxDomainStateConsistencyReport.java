/*
 * 檔案用途：outbox row 與 domain-state transition 對齊檢查報告。
 */
package com.example.exchange.domain.model.dto;

import java.time.Instant;
import java.util.List;

public record OutboxDomainStateConsistencyReport(
        int scanned,
        int issueCount,
        Instant generatedAt,
        List<OutboxDomainStateConsistencyIssue> issues
) {
    public OutboxDomainStateConsistencyReport {
        generatedAt = generatedAt == null ? Instant.now() : generatedAt;
        issues = issues == null ? List.of() : List.copyOf(issues);
    }
}
