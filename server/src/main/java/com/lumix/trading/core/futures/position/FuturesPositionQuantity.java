package com.lumix.trading.core.futures.position;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Futures position 的正數數量。
 *
 * 這不是通用 quantity 型別，而是 futures position 專用的最小值物件，因為現有 shared model
 * 沒有能精準描述 position magnitude 的型別。
 */
public record FuturesPositionQuantity(BigDecimal value) {

    public FuturesPositionQuantity {
        // 數量是 position 的核心風險輸入之一，必須在型別邊界先鎖住正數條件。
        Objects.requireNonNull(value, "value must not be null");
        value = value.stripTrailingZeros();
        if (value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("quantity must be greater than zero");
        }
    }
}
