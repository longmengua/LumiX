package com.lumix.market;

import com.lumix.common.MoneyAmount;

import java.time.Instant;
import java.util.Objects;

/**
 * 外部報價模型。
 */
public record ExternalPriceQuote(
        ExternalPriceSource source,
        String symbol,
        MoneyAmount price,
        Instant receivedAt
) {

    public ExternalPriceQuote {
        Objects.requireNonNull(source, "source must not be null");
        symbol = requireText(symbol, "symbol");
        Objects.requireNonNull(price, "price must not be null");
        Objects.requireNonNull(receivedAt, "receivedAt must not be null");
        if (!price.isPositive()) {
            throw new IllegalArgumentException("price must be greater than zero");
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
