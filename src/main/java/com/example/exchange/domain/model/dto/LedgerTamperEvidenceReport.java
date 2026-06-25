/*
 * 檔案用途：ledger hash-chain 驗證報告 DTO。
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
public class LedgerTamperEvidenceReport {

    private final long entryCount;

    private final int issueCount;

    private final Instant generatedAt;

    private final List<String> issues;
    public LedgerTamperEvidenceReport(long entryCount, int issueCount, Instant generatedAt, List<String> issues) {
        generatedAt = generatedAt == null ? Instant.now() : generatedAt;
        issues = issues == null ? List.of() : List.copyOf(issues);
    
        this.entryCount = entryCount;
        this.issueCount = issueCount;
        this.generatedAt = generatedAt;
        this.issues = issues;
    }

    public long entryCount() {
        return entryCount;
    }

    public int issueCount() {
        return issueCount;
    }

    public Instant generatedAt() {
        return generatedAt;
    }

    public List<String> issues() {
        return issues;
    }
}