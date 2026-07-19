package com.lumix.trading.core.futures.sandbox.liquidation;

import com.lumix.common.MoneyAmount;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * 僅供 liquidation simulation 使用的明確 collateral 輸入。
 *
 * 這不是帳戶餘額查詢或 reservation；允許零值以模擬無緩衝情境，但禁止負數，
 * 避免把已結算損失或帳務調整偷渡進 risk sandbox。
 */
public record FuturesSandboxSimulatedCollateral(MoneyAmount amount) {

    public FuturesSandboxSimulatedCollateral {
        Objects.requireNonNull(amount, "amount must not be null");
        if (amount.value().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("simulatedCollateral must not be negative");
        }
    }
}
