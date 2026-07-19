package com.lumix.trading.core.futures.sandbox.liquidation;

import com.lumix.trading.core.futures.account.FuturesAccount;
import com.lumix.trading.core.futures.position.FuturesPosition;
import com.lumix.trading.core.futures.sandbox.market.FuturesSandboxMockMarkPrice;
import java.util.Objects;

/**
 * 一次性 liquidation simulation 的 immutable 輸入。
 *
 * account 只用於 position ownership 與 settlement asset 標記，不會讀取真實餘額；
 * mark price 也必須由 sandbox 呼叫端明確提供，避免 simulation 依賴正式行情來源。
 */
public record FuturesSandboxLiquidationSimulationRequest(
        FuturesAccount futuresAccount,
        FuturesPosition position,
        FuturesSandboxMockMarkPrice markPrice,
        FuturesSandboxSimulatedCollateral simulatedCollateral,
        FuturesSandboxMaintenanceMarginRate maintenanceMarginRate
) {

    public FuturesSandboxLiquidationSimulationRequest {
        Objects.requireNonNull(futuresAccount, "futuresAccount must not be null");
        Objects.requireNonNull(position, "position must not be null");
        Objects.requireNonNull(markPrice, "markPrice must not be null");
        Objects.requireNonNull(simulatedCollateral, "simulatedCollateral must not be null");
        Objects.requireNonNull(maintenanceMarginRate, "maintenanceMarginRate must not be null");
        if (!futuresAccount.accountId().equals(position.futuresAccountId())) {
            throw new IllegalArgumentException("futuresAccount must own position");
        }
        if (!position.marketSymbol().equals(markPrice.marketSymbol())) {
            throw new IllegalArgumentException("markPrice market must match position market");
        }
    }
}
