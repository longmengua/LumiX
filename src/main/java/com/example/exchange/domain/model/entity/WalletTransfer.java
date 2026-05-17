/*
 * 檔案用途：領域模型或持久化實體，承載交易、帳戶、持倉與預測市場狀態。
 */
package com.example.exchange.domain.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
public class WalletTransfer {

    public enum Type {
        DEPOSIT,
        WITHDRAWAL
    }

    public enum Status {
        PENDING,
        CONFIRMED,
        FAILED,
        REVERSED,
        MANUAL_REVIEW
    }

    @Builder.Default
    private UUID id = UUID.randomUUID();

    private long uid;
    private String asset;
    private BigDecimal amount;
    private Type type;

    @Builder.Default
    private Status status = Status.PENDING;

    private String reason;

    @Builder.Default
    private Instant createdAt = Instant.now();

    @Builder.Default
    private Instant updatedAt = Instant.now();

    private Instant confirmedAt;

    public void confirm() {
        this.status = Status.CONFIRMED;
        this.confirmedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void fail(String reason) {
        this.status = Status.FAILED;
        this.reason = normalizeReason(reason);
        this.updatedAt = Instant.now();
    }

    public void reverse(String reason) {
        this.status = Status.REVERSED;
        this.reason = normalizeReason(reason);
        this.updatedAt = Instant.now();
    }

    public void markManualReview(String reason) {
        this.status = Status.MANUAL_REVIEW;
        this.reason = normalizeReason(reason);
        this.updatedAt = Instant.now();
    }

    private static String normalizeReason(String reason) {
        return reason == null || reason.isBlank() ? "manual" : reason.trim();
    }
}
