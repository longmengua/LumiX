package com.lumix.account;

import java.util.Locale;
import java.util.Objects;

/**
 * 資產代號，例如 USDT、BTC。
 * 統一轉成大寫，避免同一資產因大小寫差異被視為不同符號。
 */
public record AssetSymbol(String value) {

    public AssetSymbol {
        // 資產代號只允許非空文字，並在建立時標準化。
        Objects.requireNonNull(value, "value must not be null");
        value = value.trim().toUpperCase(Locale.ROOT);
        if (value.isEmpty()) {
            throw new IllegalArgumentException("asset must not be blank");
        }
    }
}
