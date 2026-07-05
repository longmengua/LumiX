package com.lumix.wallet;

import com.lumix.account.AssetSymbol;

import java.util.List;
import java.util.Objects;

/**
 * 錢包資產展示模型。
 */
public record WalletAsset(
        AssetSymbol asset,
        String displayName,
        List<ChainType> supportedChains,
        boolean depositEnabled,
        boolean withdrawEnabled
) {

    public WalletAsset {
        Objects.requireNonNull(asset, "asset must not be null");
        displayName = requireText(displayName, "displayName");
        Objects.requireNonNull(supportedChains, "supportedChains must not be null");
        supportedChains = List.copyOf(supportedChains);
        if (supportedChains.isEmpty()) {
            throw new IllegalArgumentException("supportedChains must not be empty");
        }
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
