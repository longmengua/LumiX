/*
 * 檔案用途：做市商 hedge venue idempotency unresolved outcome operator report。
 */
package com.example.exchange.domain.model.dto;

import java.time.Instant;
import java.util.List;

public record HedgeVenueIdempotencyReport(
        int issueCount,
        Instant generatedAt,
        List<HedgeVenueIdempotencyIssue> issues
) {
    public HedgeVenueIdempotencyReport {
        generatedAt = generatedAt == null ? Instant.now() : generatedAt;
        issues = issues == null ? List.of() : List.copyOf(issues);
    }
}
