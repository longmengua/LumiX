/*
 * 檔案用途：做市商 hedge venue idempotency unresolved outcome operator report。
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
public class HedgeVenueIdempotencyReport {

    private final int issueCount;

    private final Instant generatedAt;

    private final List<HedgeVenueIdempotencyIssue> issues;
    public HedgeVenueIdempotencyReport(int issueCount, Instant generatedAt, List<HedgeVenueIdempotencyIssue> issues) {
        generatedAt = generatedAt == null ? Instant.now() : generatedAt;
        issues = issues == null ? List.of() : List.copyOf(issues);
    
        this.issueCount = issueCount;
        this.generatedAt = generatedAt;
        this.issues = issues;
    }

    public int issueCount() {
        return issueCount;
    }

    public Instant generatedAt() {
        return generatedAt;
    }

    public List<HedgeVenueIdempotencyIssue> issues() {
        return issues;
    }
}