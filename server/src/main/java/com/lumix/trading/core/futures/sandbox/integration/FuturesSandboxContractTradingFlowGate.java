package com.lumix.trading.core.futures.sandbox.integration;

import com.lumix.trading.core.futures.sandbox.contract.FuturesSandboxContractEligibilityDecision;
import com.lumix.trading.core.futures.sandbox.contract.FuturesSandboxContractEligibilityResult;
import com.lumix.trading.core.futures.sandbox.liquidation.FuturesSandboxLiquidationSimulationDecision;
import com.lumix.trading.core.futures.sandbox.liquidation.FuturesSandboxLiquidationSimulationResult;
import java.util.Objects;

/**
 * P20-T01 的 pure contract-trading sandbox integration gate。
 *
 * 此 gate 只整合前序 immutable 結果；不重新撮合、不建立 fill/trade、不更新 position、
 * balance、ledger 或 settlement，因此成功不等於可執行合約交易。
 */
public final class FuturesSandboxContractTradingFlowGate {
    public FuturesSandboxContractTradingFlowResult evaluate(
            FuturesSandboxContractEligibilityResult contractResult,
            FuturesSandboxLiquidationSimulationResult liquidationResult
    ) {
        Objects.requireNonNull(contractResult, "contractResult must not be null");
        Objects.requireNonNull(liquidationResult, "liquidationResult must not be null");
        if (contractResult.decision() != FuturesSandboxContractEligibilityDecision.ELIGIBLE_FOR_SANDBOX_INSPECTION) {
            return FuturesSandboxContractTradingFlowResult.rejected(FuturesSandboxContractTradingFlowReason.CONTRACT_INSPECTION_REJECTED);
        }
        if (liquidationResult.decision() == FuturesSandboxLiquidationSimulationDecision.LIQUIDATION_SIMULATED) {
            return FuturesSandboxContractTradingFlowResult.rejected(FuturesSandboxContractTradingFlowReason.LIQUIDATION_SIMULATED);
        }
        return FuturesSandboxContractTradingFlowResult.eligible();
    }
}
