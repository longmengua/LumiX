/*
 * 檔案用途：hedge venue adapter 回傳的對沖送單結果。
 */
package com.example.exchange.domain.model.dto;

import java.time.Instant;

public record HedgeOrderResult(
        boolean accepted,
        String venueOrderId,
        String reason,
        boolean retryable,
        Instant submittedAt
) {
    public static HedgeOrderResult accepted(String venueOrderId) {
        return new HedgeOrderResult(true, venueOrderId, null, false, Instant.now());
    }

    public static HedgeOrderResult rejected(String reason) {
        return new HedgeOrderResult(false, null, reason, false, Instant.now());
    }

    public static HedgeOrderResult retryableRejected(String reason) {
        return new HedgeOrderResult(false, null, reason, true, Instant.now());
    }
}
