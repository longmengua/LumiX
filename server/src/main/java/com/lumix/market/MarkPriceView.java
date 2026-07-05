package com.lumix.market;

import com.lumix.common.MoneyAmount;

import java.time.Instant;
import java.util.Objects;

/**
 * 標記價格展示模型。
 */
public record MarkPriceView(
        String symbol,
        MoneyAmount markPrice,
        MoneyAmount indexPrice,
        Instant calculatedAt
) {

    public MarkPriceView {
        symbol = requireText(symbol, "symbol");
        Objects.requireNonNull(markPrice, "markPrice must not be null");
        Objects.requireNonNull(indexPrice, "indexPrice must not be null");
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
