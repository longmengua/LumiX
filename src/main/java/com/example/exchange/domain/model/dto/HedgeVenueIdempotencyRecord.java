/*
 * 檔案用途：保存 hedge venue effectful submit 的 idempotency 狀態。
 */
package com.example.exchange.domain.model.dto;

public record HedgeVenueIdempotencyRecord(
        String refId,
        String fingerprint,
        boolean completed,
        HedgeOrderResult result
) {
}
