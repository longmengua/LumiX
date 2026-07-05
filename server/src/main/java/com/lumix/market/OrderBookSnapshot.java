package com.lumix.market;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * 深度快照模型。
 */
public record OrderBookSnapshot(
        String symbol,
        SymbolType type,
        Instant asOf,
        List<OrderBookLevel> bids,
        List<OrderBookLevel> asks
) {

    public OrderBookSnapshot {
        symbol = requireText(symbol, "symbol");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(asOf, "asOf must not be null");
        Objects.requireNonNull(bids, "bids must not be null");
        Objects.requireNonNull(asks, "asks must not be null");
        bids = List.copyOf(bids);
        asks = List.copyOf(asks);
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
