package com.lumix.trading.core.futures.position;

import java.util.Locale;
import java.util.Objects;

/**
 * Futures market 的穩定 identity。
 *
 * 這不是 market 展示模型，也不是 base / quote / tradingEnabled 的聚合，僅保留 position 需要的
 * symbol identity 與正規化規則，避免把過重的 trading model 直接嵌進 futures domain。
 */
public record FuturesMarketSymbol(String value) {

    public FuturesMarketSymbol {
        // Position 只需要穩定 symbol identity，因此先在 value object 邊界做 trim 與正規化。
        Objects.requireNonNull(value, "value must not be null");
        value = value.trim().toUpperCase(Locale.ROOT);
        if (value.isEmpty()) {
            throw new IllegalArgumentException("marketSymbol must not be blank");
        }
    }
}
