package com.lumix.wallet;

import com.lumix.account.AssetSymbol;

import java.util.Objects;

/**
 * 鏈配置模型。
 */
public record WalletChainConfig(
        AssetSymbol asset,
        ChainType chain,
        int requiredConfirmations,
        boolean depositEnabled,
        boolean withdrawEnabled
) {

    public WalletChainConfig {
        Objects.requireNonNull(asset, "asset must not be null");
        Objects.requireNonNull(chain, "chain must not be null");
        if (requiredConfirmations < 0) {
            throw new IllegalArgumentException("requiredConfirmations must not be negative");
        }
    }
}
