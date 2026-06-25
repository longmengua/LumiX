/*
 * 檔案用途：做市商 hedge venue idempotency operator report 的單筆未解項目。
 */
package com.example.exchange.domain.model.dto;

import java.time.Instant;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;


@Data
@Builder
@Jacksonized
public class HedgeVenueIdempotencyIssue {

    private final String refId;

    private final String reason;

    private final boolean completed;

    private final boolean retryable;

    private final String venueOrderId;

    private final Instant submittedAt;
    public HedgeVenueIdempotencyIssue(String refId, String reason, boolean completed, boolean retryable, String venueOrderId, Instant submittedAt) {
        this.refId = refId;
        this.reason = reason;
        this.completed = completed;
        this.retryable = retryable;
        this.venueOrderId = venueOrderId;
        this.submittedAt = submittedAt;
    }

    public String refId() {
        return refId;
    }

    public String reason() {
        return reason;
    }

    public boolean completed() {
        return completed;
    }

    public boolean retryable() {
        return retryable;
    }

    public String venueOrderId() {
        return venueOrderId;
    }

    public Instant submittedAt() {
        return submittedAt;
    }
}