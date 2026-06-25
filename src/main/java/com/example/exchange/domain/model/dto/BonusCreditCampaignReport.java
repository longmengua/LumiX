/*
 * 檔案用途：體驗金活動營運報表 DTO，彙總 campaign grant 狀態與剩餘額度。
 */
package com.example.exchange.domain.model.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;


@Data
@Builder
@Jacksonized
public class BonusCreditCampaignReport {

    private final String campaignId;

    private final String asset;

    private final BigDecimal totalGranted;

    private final BigDecimal totalRemaining;

    private final BigDecimal activeOriginalAmount;

    private final BigDecimal consumedOriginalAmount;

    private final BigDecimal expiredOriginalAmount;

    private final BigDecimal clawedBackOriginalAmount;

    private final int activeGrantCount;

    private final int consumedGrantCount;

    private final int expiredGrantCount;

    private final int clawedBackGrantCount;

    private final int userCount;

    private final Instant nextExpiryAt;

    private final Instant generatedAt;

    private final List<BonusCreditGrant> grants;
    public BonusCreditCampaignReport(String campaignId, String asset, BigDecimal totalGranted, BigDecimal totalRemaining, BigDecimal activeOriginalAmount, BigDecimal consumedOriginalAmount, BigDecimal expiredOriginalAmount, BigDecimal clawedBackOriginalAmount, int activeGrantCount, int consumedGrantCount, int expiredGrantCount, int clawedBackGrantCount, int userCount, Instant nextExpiryAt, Instant generatedAt, List<BonusCreditGrant> grants) {
        campaignId = campaignId == null || campaignId.isBlank() ? "ALL" : campaignId.trim();
        asset = asset == null || asset.isBlank() ? "ALL" : asset.trim().toUpperCase();
        totalGranted = safe(totalGranted);
        totalRemaining = safe(totalRemaining);
        activeOriginalAmount = safe(activeOriginalAmount);
        consumedOriginalAmount = safe(consumedOriginalAmount);
        expiredOriginalAmount = safe(expiredOriginalAmount);
        clawedBackOriginalAmount = safe(clawedBackOriginalAmount);
        grants = grants == null ? List.of() : List.copyOf(grants);
    
        this.campaignId = campaignId;
        this.asset = asset;
        this.totalGranted = totalGranted;
        this.totalRemaining = totalRemaining;
        this.activeOriginalAmount = activeOriginalAmount;
        this.consumedOriginalAmount = consumedOriginalAmount;
        this.expiredOriginalAmount = expiredOriginalAmount;
        this.clawedBackOriginalAmount = clawedBackOriginalAmount;
        this.activeGrantCount = activeGrantCount;
        this.consumedGrantCount = consumedGrantCount;
        this.expiredGrantCount = expiredGrantCount;
        this.clawedBackGrantCount = clawedBackGrantCount;
        this.userCount = userCount;
        this.nextExpiryAt = nextExpiryAt;
        this.generatedAt = generatedAt;
        this.grants = grants;
    }

    private static BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    public String campaignId() {
        return campaignId;
    }

    public String asset() {
        return asset;
    }

    public BigDecimal totalGranted() {
        return totalGranted;
    }

    public BigDecimal totalRemaining() {
        return totalRemaining;
    }

    public BigDecimal activeOriginalAmount() {
        return activeOriginalAmount;
    }

    public BigDecimal consumedOriginalAmount() {
        return consumedOriginalAmount;
    }

    public BigDecimal expiredOriginalAmount() {
        return expiredOriginalAmount;
    }

    public BigDecimal clawedBackOriginalAmount() {
        return clawedBackOriginalAmount;
    }

    public int activeGrantCount() {
        return activeGrantCount;
    }

    public int consumedGrantCount() {
        return consumedGrantCount;
    }

    public int expiredGrantCount() {
        return expiredGrantCount;
    }

    public int clawedBackGrantCount() {
        return clawedBackGrantCount;
    }

    public int userCount() {
        return userCount;
    }

    public Instant nextExpiryAt() {
        return nextExpiryAt;
    }

    public Instant generatedAt() {
        return generatedAt;
    }

    public List<BonusCreditGrant> grants() {
        return grants;
    }
}