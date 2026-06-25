/*
 * 檔案用途：hedge venue adapter 回傳的對沖送單結果。
 */
package com.example.exchange.domain.model.dto;

import java.time.Instant;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;


@Data
@Builder
@Jacksonized
public class HedgeOrderResult {

    private final boolean accepted;

    private final String venueOrderId;

    private final String reason;

    private final boolean retryable;

    private final Instant submittedAt;
    public HedgeOrderResult(boolean accepted, String venueOrderId, String reason, boolean retryable, Instant submittedAt) {
        this.accepted = accepted;
        this.venueOrderId = venueOrderId;
        this.reason = reason;
        this.retryable = retryable;
        this.submittedAt = submittedAt;
    }

    public static HedgeOrderResult accepted(String venueOrderId) {
        return new HedgeOrderResult(true, venueOrderId, null, false, Instant.now());
    }

    public static HedgeOrderResult rejected(String reason) {
        return new HedgeOrderResult(false, null, reason, false, Instant.now());
    }

    public static HedgeOrderResult retryableRejected(String reason) {
        return new HedgeOrderResult(false, null, reason, true, Instant.now());
    }

    public boolean accepted() {
        return accepted;
    }

    public String venueOrderId() {
        return venueOrderId;
    }

    public String reason() {
        return reason;
    }

    public boolean retryable() {
        return retryable;
    }

    public Instant submittedAt() {
        return submittedAt;
    }
}