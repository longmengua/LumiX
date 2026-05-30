/*
 * 檔案用途：ledger hash-chain 驗證報告 DTO。
 */
package com.example.exchange.domain.model.dto;

import java.time.Instant;
import java.util.List;

public record LedgerTamperEvidenceReport(
        long entryCount,
        int issueCount,
        Instant generatedAt,
        List<String> issues
) {
    public LedgerTamperEvidenceReport {
        generatedAt = generatedAt == null ? Instant.now() : generatedAt;
        issues = issues == null ? List.of() : List.copyOf(issues);
    }
}
