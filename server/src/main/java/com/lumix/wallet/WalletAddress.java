package com.lumix.wallet;

import com.lumix.account.AssetSymbol;
import com.lumix.account.UserId;

import java.time.Instant;
import java.util.Objects;

/**
 * 使用者錢包地址模型。
 */
public record WalletAddress(
        UserId userId,
        AssetSymbol asset,
        ChainType chain,
        String address,
        WalletAddressStatus status,
        Instant createdAt
) {

    public WalletAddress {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(asset, "asset must not be null");
        Objects.requireNonNull(chain, "chain must not be null");
        address = requireText(address, "address");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
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
