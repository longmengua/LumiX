package com.lumix.market;

import com.lumix.common.MoneyAmount;

import java.time.Instant;
import java.util.Objects;

/**
 * 指數價格展示模型。
 */
public record PriceIndexView(
        String symbol,
        MoneyAmount indexPrice,
        int sourceCount,
        Instant calculatedAt
) {

    public PriceIndexView {
        symbol = requireText(symbol, "symbol");
        Objects.requireNonNull(indexPrice, "indexPrice must not be null");
        if (sourceCount < 0) {
            throw new IllegalArgumentException("sourceCount must not be negative");
        }
        Objects.requireNonNull(calculatedAt, "calculatedAt must not be null");
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
