package com.lumix.market;

import com.lumix.common.MoneyAmount;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * 24h ticker 展示模型。
 */
public record Ticker24hView(
        String symbol,
        MoneyAmount lastPrice,
        MoneyAmount openPrice,
        MoneyAmount highPrice,
        MoneyAmount lowPrice,
        MoneyAmount volume,
        MoneyAmount quoteVolume,
        BigDecimal changePercent,
        Instant asOf
) {

    public Ticker24hView {
        symbol = requireText(symbol, "symbol");
        Objects.requireNonNull(lastPrice, "lastPrice must not be null");
        Objects.requireNonNull(openPrice, "openPrice must not be null");
        Objects.requireNonNull(highPrice, "highPrice must not be null");
        Objects.requireNonNull(lowPrice, "lowPrice must not be null");
        Objects.requireNonNull(volume, "volume must not be null");
        Objects.requireNonNull(quoteVolume, "quoteVolume must not be null");
        Objects.requireNonNull(changePercent, "changePercent must not be null");
        Objects.requireNonNull(asOf, "asOf must not be null");
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
