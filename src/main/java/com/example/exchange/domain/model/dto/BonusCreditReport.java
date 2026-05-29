/*
 * 檔案用途：體驗金營運報表 DTO，彙總 grant batch 狀態與剩餘額度。
 */
package com.example.exchange.domain.model.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record BonusCreditReport(
        long uid,
        String asset,
        BigDecimal totalGranted,
        BigDecimal totalRemaining,
        BigDecimal activeOriginalAmount,
        BigDecimal consumedOriginalAmount,
        BigDecimal expiredOriginalAmount,
        BigDecimal clawedBackOriginalAmount,
        int activeGrantCount,
        int consumedGrantCount,
        int expiredGrantCount,
        int clawedBackGrantCount,
        Instant nextExpiryAt,
        Instant generatedAt,
        List<BonusCreditGrant> grants
) {
    public BonusCreditReport {
        totalGranted = safe(totalGranted);
        totalRemaining = safe(totalRemaining);
        activeOriginalAmount = safe(activeOriginalAmount);
        consumedOriginalAmount = safe(consumedOriginalAmount);
        expiredOriginalAmount = safe(expiredOriginalAmount);
        clawedBackOriginalAmount = safe(clawedBackOriginalAmount);
        grants = grants == null ? List.of() : List.copyOf(grants);
    }

    private static BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
