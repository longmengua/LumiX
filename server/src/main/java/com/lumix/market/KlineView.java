package com.lumix.market;

import com.lumix.common.MoneyAmount;

import java.time.Instant;
import java.util.Objects;

/**
 * K 線展示模型。
 */
public record KlineView(
        String symbol,
        KlineInterval interval,
        Instant openTime,
        Instant closeTime,
        MoneyAmount openPrice,
        MoneyAmount highPrice,
        MoneyAmount lowPrice,
        MoneyAmount closePrice,
        MoneyAmount volume
) {

    public KlineView {
        symbol = requireText(symbol, "symbol");
        Objects.requireNonNull(interval, "interval must not be null");
        Objects.requireNonNull(openTime, "openTime must not be null");
        Objects.requireNonNull(closeTime, "closeTime must not be null");
        Objects.requireNonNull(openPrice, "openPrice must not be null");
        Objects.requireNonNull(highPrice, "highPrice must not be null");
        Objects.requireNonNull(lowPrice, "lowPrice must not be null");
        Objects.requireNonNull(closePrice, "closePrice must not be null");
        Objects.requireNonNull(volume, "volume must not be null");
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
