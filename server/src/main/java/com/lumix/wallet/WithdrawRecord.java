package com.lumix.wallet;

import com.lumix.account.AssetSymbol;
import com.lumix.account.UserId;
import com.lumix.common.MoneyAmount;
import com.lumix.common.RequestId;

import java.time.Instant;
import java.util.Objects;

/**
 * 提現紀錄模型。
 */
public record WithdrawRecord(
        String id,
        RequestId requestId,
        UserId userId,
        AssetSymbol asset,
        ChainType chain,
        String address,
        MoneyAmount amount,
        MoneyAmount fee,
        String txHash,
        WithdrawStatus status,
        Instant createdAt,
        Instant updatedAt
) {

    public WithdrawRecord {
        id = requireText(id, "id");
        Objects.requireNonNull(requestId, "requestId must not be null");
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(asset, "asset must not be null");
        Objects.requireNonNull(chain, "chain must not be null");
        address = requireText(address, "address");
        Objects.requireNonNull(amount, "amount must not be null");
        if (!amount.isPositive()) {
            throw new IllegalArgumentException("amount must be greater than zero");
        }
        if (fee != null && fee.isNegative()) {
            throw new IllegalArgumentException("fee must not be negative");
        }
        txHash = normalizeOptionalText(txHash);
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }

    private static String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
