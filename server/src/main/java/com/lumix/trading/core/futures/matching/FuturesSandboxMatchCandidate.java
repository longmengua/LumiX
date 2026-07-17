package com.lumix.trading.core.futures.matching;

import com.lumix.trading.core.futures.order.FuturesSandboxOrder;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Futures sandbox 中可供下一階段處理的 immutable 配對候選。
 *
 * 此型別沒有 trade price、fill ID、position 或 PnL 欄位，避免 T02 的共用規則被誤接為 matching execution。
 */
public record FuturesSandboxMatchCandidate(
        FuturesSandboxOrder buyOrder,
        FuturesSandboxOrder sellOrder,
        FuturesSandboxOrder makerOrder,
        BigDecimal matchedQuantity
) {

    public FuturesSandboxMatchCandidate {
        Objects.requireNonNull(buyOrder, "buyOrder must not be null");
        Objects.requireNonNull(sellOrder, "sellOrder must not be null");
        Objects.requireNonNull(makerOrder, "makerOrder must not be null");
        Objects.requireNonNull(matchedQuantity, "matchedQuantity must not be null");
    }
}
