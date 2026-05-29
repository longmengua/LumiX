/*
 * 檔案用途：做市商 hedge venue idempotency operator report 的單筆未解項目。
 */
package com.example.exchange.domain.model.dto;

import java.time.Instant;

public record HedgeVenueIdempotencyIssue(
        String refId,
        String reason,
        boolean completed,
        boolean retryable,
        String venueOrderId,
        Instant submittedAt
) {
}
