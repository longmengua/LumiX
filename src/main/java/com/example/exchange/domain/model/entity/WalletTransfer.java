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

/**
 * 入金/出金狀態機。
 *
 * <p>這個模型只描述資金移動的業務狀態，不直接修改帳戶餘額；
 * 帳戶與 ledger 更新由應用服務協調完成。</p>
 */
@Data
@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
public class WalletTransfer {

    /** Transfer 方向：DEPOSIT 增加可用餘額，WITHDRAWAL 扣減可用餘額。 */
    public enum Type {
        DEPOSIT,
        WITHDRAWAL
    }

    /** Transfer lifecycle；終態包含 CONFIRMED、FAILED、REVERSED、MANUAL_REVIEW。 */
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

    /** 標記外部資金動作已確認。 */
    public void confirm() {
        this.status = Status.CONFIRMED;
        this.confirmedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    /** 標記資金動作失敗；不應再自動扣款或入帳。 */
    public void fail(String reason) {
        this.status = Status.FAILED;
        this.reason = normalizeReason(reason);
        this.updatedAt = Instant.now();
    }

    /** 標記資金動作已沖正，通常由人工補償或外部 callback 觸發。 */
    public void reverse(String reason) {
        this.status = Status.REVERSED;
        this.reason = normalizeReason(reason);
        this.updatedAt = Instant.now();
    }

    /** 標記需要人工覆核；常見於全站出金暫停或可疑交易。 */
    public void markManualReview(String reason) {
        this.status = Status.MANUAL_REVIEW;
        this.reason = normalizeReason(reason);
        this.updatedAt = Instant.now();
    }

    private static String normalizeReason(String reason) {
        return reason == null || reason.isBlank() ? "manual" : reason.trim();
    }
}
