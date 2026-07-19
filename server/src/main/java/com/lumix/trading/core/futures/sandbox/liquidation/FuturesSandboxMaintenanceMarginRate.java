package com.lumix.trading.core.futures.sandbox.liquidation;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Liquidation simulation 使用的 maintenance-margin rate。
 *
 * rate 由受限 sandbox 呼叫端明確提供，並非正式風控政策；要求介於 0 與 1 之間，
 * 避免零門檻或超過全部 notional 的輸入掩蓋 simulation 本身的行為。
 */
public record FuturesSandboxMaintenanceMarginRate(BigDecimal value) {

    public FuturesSandboxMaintenanceMarginRate {
        Objects.requireNonNull(value, "value must not be null");
        value = value.stripTrailingZeros();
        if (value.signum() <= 0 || value.compareTo(BigDecimal.ONE) >= 0) {
            throw new IllegalArgumentException("maintenanceMarginRate must be greater than zero and less than one");
        }
    }
}
