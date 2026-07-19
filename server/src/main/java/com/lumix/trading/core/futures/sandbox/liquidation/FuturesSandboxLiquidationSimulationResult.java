package com.lumix.trading.core.futures.sandbox.liquidation;

import com.lumix.account.AssetSymbol;
import com.lumix.common.MoneyAmount;
import com.lumix.trading.core.futures.position.FuturesPositionId;
import com.lumix.trading.core.futures.sandbox.market.FuturesSandboxMockMarkPrice;
import java.util.Objects;

/**
 * Liquidation simulation 的 immutable 數學結果。
 *
 * equity 與 maintenanceMargin 僅為本次輸入下的試算數字，不能解讀為帳戶可用餘額、
 * 強平價、破產價或任何可執行的 liquidation instruction。
 */
public record FuturesSandboxLiquidationSimulationResult(
        FuturesPositionId positionId,
        AssetSymbol settlementAsset,
        FuturesSandboxMockMarkPrice markPrice,
        FuturesSandboxSimulatedCollateral simulatedCollateral,
        FuturesSandboxMaintenanceMarginRate maintenanceMarginRate,
        MoneyAmount simulatedEquity,
        MoneyAmount simulatedMaintenanceMargin,
        FuturesSandboxLiquidationSimulationDecision decision,
        FuturesSandboxLiquidationSimulationReason reason
) {

    public FuturesSandboxLiquidationSimulationResult {
        Objects.requireNonNull(positionId, "positionId must not be null");
        Objects.requireNonNull(settlementAsset, "settlementAsset must not be null");
        Objects.requireNonNull(markPrice, "markPrice must not be null");
        Objects.requireNonNull(simulatedCollateral, "simulatedCollateral must not be null");
        Objects.requireNonNull(maintenanceMarginRate, "maintenanceMarginRate must not be null");
        Objects.requireNonNull(simulatedEquity, "simulatedEquity must not be null");
        Objects.requireNonNull(simulatedMaintenanceMargin, "simulatedMaintenanceMargin must not be null");
        Objects.requireNonNull(decision, "decision must not be null");
        Objects.requireNonNull(reason, "reason must not be null");
        if (decision == FuturesSandboxLiquidationSimulationDecision.NOT_LIQUIDATABLE
                && reason != FuturesSandboxLiquidationSimulationReason.EQUITY_ABOVE_MAINTENANCE_MARGIN) {
            throw new IllegalArgumentException("non-liquidatable result must use equity-above reason");
        }
        if (decision == FuturesSandboxLiquidationSimulationDecision.LIQUIDATION_SIMULATED
                && reason != FuturesSandboxLiquidationSimulationReason.EQUITY_AT_OR_BELOW_MAINTENANCE_MARGIN) {
            throw new IllegalArgumentException("simulated liquidation result must use equity-at-or-below reason");
        }
    }
}
