package com.lumix.admin.asset;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * 管理端空投的已驗證輸入。
 *
 * <p>活動識別碼是業務去重鍵的一部分，不能由畫面顯示名稱替代；同一活動不能對同一帳戶資產重複發放。</p>
 */
public record AdminAirdropCommand(
        String targetUserId,
        String assetSymbol,
        BigDecimal amount,
        String activityId,
        String reason
) {
    public AdminAirdropCommand {
        targetUserId = text(targetUserId, "targetUserId", 64);
        assetSymbol = text(assetSymbol, "assetSymbol", 32).toUpperCase(java.util.Locale.ROOT);
        activityId = text(activityId, "activityId", 64);
        reason = text(reason, "reason", 256);
        amount = Objects.requireNonNull(amount, "amount must not be null").stripTrailingZeros();
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be greater than zero");
        }
    }

    private static String text(String value, String field, int maxLength) {
        String normalized = Objects.requireNonNull(value, field + " must not be null").trim();
        if (normalized.isEmpty() || normalized.length() > maxLength) {
            throw new IllegalArgumentException(field + " must contain 1 to " + maxLength + " characters");
        }
        return normalized;
    }
}
