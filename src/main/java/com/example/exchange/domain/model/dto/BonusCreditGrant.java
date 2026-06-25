/*
 * 檔案用途：體驗金批次 read model，追蹤 grant、剩餘額度與到期狀態。
 */
package com.example.exchange.domain.model.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;


/**
 * 一筆體驗金批次。
 *
 * <p>Ledger 是資金真相；此 read model 負責保存到期日與剩餘可用額，
 * 讓 consume/expire 可以按批次審計，不需要從歷史 ledger 反推活動批次。</p>
 */
@Data
@Builder
@Jacksonized
public class BonusCreditGrant {

    private final UUID id;

    private final long uid;

    private final String asset;

    private final BigDecimal originalAmount;

    private final BigDecimal remainingAmount;

    private final String campaignId;

    private final String status;

    private final Instant grantedAt;

    private final Instant expiresAt;

    private final Instant updatedAt;
    public BonusCreditGrant(UUID id, long uid, String asset, BigDecimal originalAmount, BigDecimal remainingAmount, String campaignId, String status, Instant grantedAt, Instant expiresAt, Instant updatedAt) {
        id = id == null ? UUID.randomUUID() : id;
        originalAmount = originalAmount == null ? BigDecimal.ZERO : originalAmount;
        remainingAmount = remainingAmount == null ? originalAmount : remainingAmount;
        status = status == null || status.isBlank() ? ACTIVE : status;
        grantedAt = grantedAt == null ? Instant.now() : grantedAt;
        updatedAt = updatedAt == null ? grantedAt : updatedAt;
    
        this.id = id;
        this.uid = uid;
        this.asset = asset;
        this.originalAmount = originalAmount;
        this.remainingAmount = remainingAmount;
        this.campaignId = campaignId;
        this.status = status;
        this.grantedAt = grantedAt;
        this.expiresAt = expiresAt;
        this.updatedAt = updatedAt;
    }

    public static final String ACTIVE = "ACTIVE";
    public static final String CONSUMED = "CONSUMED";
    public static final String EXPIRED = "EXPIRED";
    public static final String CLAWED_BACK = "CLAWED_BACK";

    public BonusCreditGrant withRemaining(BigDecimal remaining, Instant updatedAt) {
        String nextStatus = remaining == null || remaining.signum() <= 0 ? CONSUMED : ACTIVE;
        return new BonusCreditGrant(
                id,
                uid,
                asset,
                originalAmount,
                remaining == null ? BigDecimal.ZERO : remaining,
                campaignId,
                nextStatus,
                grantedAt,
                expiresAt,
                updatedAt
        );
    }

    public BonusCreditGrant expire(Instant updatedAt) {
        return withStatus(EXPIRED, BigDecimal.ZERO, updatedAt);
    }

    public BonusCreditGrant clawBack(Instant updatedAt) {
        return withStatus(CLAWED_BACK, BigDecimal.ZERO, updatedAt);
    }

    private BonusCreditGrant withStatus(String status, BigDecimal remaining, Instant updatedAt) {
        return new BonusCreditGrant(
                id,
                uid,
                asset,
                originalAmount,
                remaining,
                campaignId,
                status,
                grantedAt,
                expiresAt,
                updatedAt
        );
    }

    public UUID id() {
        return id;
    }

    public long uid() {
        return uid;
    }

    public String asset() {
        return asset;
    }

    public BigDecimal originalAmount() {
        return originalAmount;
    }

    public BigDecimal remainingAmount() {
        return remainingAmount;
    }

    public String campaignId() {
        return campaignId;
    }

    public String status() {
        return status;
    }

    public Instant grantedAt() {
        return grantedAt;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}