package com.lumix.trading.core.futures.pnl;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Futures sandbox PnL valuation 使用的正數價格。
 *
 * 此型別只封裝由呼叫端提供的 mark / close preview 價格；T04 不定義價格來源，T05 才會處理 mock mark-price runtime。
 */
public record FuturesSandboxPnlPrice(BigDecimal value) {

    public FuturesSandboxPnlPrice {
        Objects.requireNonNull(value, "value must not be null");
        value = value.stripTrailingZeros();
        if (value.signum() <= 0) {
            throw new IllegalArgumentException("pnlPrice must be positive");
        }
    }
}
