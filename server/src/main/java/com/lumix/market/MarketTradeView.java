package com.lumix.market;

import com.lumix.common.MoneyAmount;

import java.time.Instant;
import java.util.Objects;

/**
 * 市場成交展示模型。
 */
public record MarketTradeView(
        String tradeId,
        String symbol,
        MoneyAmount price,
        MoneyAmount quantity,
        Instant executedAt
) {

    public MarketTradeView {
        tradeId = requireText(tradeId, "tradeId");
        symbol = requireText(symbol, "symbol");
        Objects.requireNonNull(price, "price must not be null");
        Objects.requireNonNull(quantity, "quantity must not be null");
        Objects.requireNonNull(executedAt, "executedAt must not be null");
        if (!price.isPositive()) {
            throw new IllegalArgumentException("price must be greater than zero");
        }
        if (!quantity.isPositive()) {
            throw new IllegalArgumentException("quantity must be greater than zero");
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
