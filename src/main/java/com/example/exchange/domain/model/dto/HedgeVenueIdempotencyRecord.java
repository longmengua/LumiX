/*
 * 檔案用途：保存 hedge venue effectful submit 的 idempotency 狀態。
 */
package com.example.exchange.domain.model.dto;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;


@Data
@Builder
@Jacksonized
public class HedgeVenueIdempotencyRecord {

    private final String refId;

    private final String fingerprint;

    private final boolean completed;

    private final HedgeOrderResult result;
    public HedgeVenueIdempotencyRecord(String refId, String fingerprint, boolean completed, HedgeOrderResult result) {
        this.refId = refId;
        this.fingerprint = fingerprint;
        this.completed = completed;
        this.result = result;
    }

    public String refId() {
        return refId;
    }

    public String fingerprint() {
        return fingerprint;
    }

    public boolean completed() {
        return completed;
    }

    public HedgeOrderResult result() {
        return result;
    }
}