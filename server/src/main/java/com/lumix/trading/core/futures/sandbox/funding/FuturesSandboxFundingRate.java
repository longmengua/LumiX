package com.lumix.trading.core.futures.sandbox.funding;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Futures sandbox funding rate 的 decimal 輸入。
 *
 * 正負號保留市場方向語意：正值代表 LONG 支付、SHORT 收取；負值則反轉。T05 不引入 rate cap、
 * interval 或 rounding policy，這些 production 規則必須在後續經人類審核後另行定義。
 */
public record FuturesSandboxFundingRate(BigDecimal value) {

    public FuturesSandboxFundingRate {
        Objects.requireNonNull(value, "value must not be null");
        value = value.stripTrailingZeros();
    }
}
