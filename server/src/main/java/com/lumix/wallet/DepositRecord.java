package com.lumix.wallet;

import com.lumix.account.AssetSymbol;
import com.lumix.account.UserId;
import com.lumix.common.MoneyAmount;

import java.time.Instant;
import java.util.Objects;

/**
 * 充值紀錄模型。
 */
public record DepositRecord(
        String id,
        UserId userId,
        AssetSymbol asset,
        ChainType chain,
        String txHash,
        String address,
        MoneyAmount amount,
        int confirmations,
        DepositStatus status,
        Instant createdAt,
        Instant updatedAt
) {

    public DepositRecord {
        id = requireText(id, "id");
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(asset, "asset must not be null");
        Objects.requireNonNull(chain, "chain must not be null");
        txHash = requireText(txHash, "txHash");
        address = requireText(address, "address");
        Objects.requireNonNull(amount, "amount must not be null");
        if (!amount.isPositive()) {
            throw new IllegalArgumentException("amount must be greater than zero");
        }
        if (confirmations < 0) {
            throw new IllegalArgumentException("confirmations must not be negative");
        }
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
}
