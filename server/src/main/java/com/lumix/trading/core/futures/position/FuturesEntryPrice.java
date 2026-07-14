package com.lumix.trading.core.futures.position;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Futures position 的 entry price。
 *
 * 這不是通用 price 型別，而是 futures position 專用的最小值物件，避免把尚未完成設計的
 * pricing runtime 直接引進 core model。
 */
public record FuturesEntryPrice(BigDecimal value) {

    public FuturesEntryPrice {
        // entry price 只代表 position 的建立價格，必須保持正數，不能接受 0 或負值。
        Objects.requireNonNull(value, "value must not be null");
        value = value.stripTrailingZeros();
        if (value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("entryPrice must be greater than zero");
        }
    }
}
