package com.lumix.trading.core.futures.sandbox.integration;

import com.lumix.trading.core.futures.sandbox.contract.FuturesSandboxContractEligibilityResult;
import com.lumix.trading.core.futures.sandbox.liquidation.FuturesSandboxLiquidationSimulationResult;
import java.util.Objects;

/**
 * P20-T03 的 pure contract-trading sandbox flow 重放對帳。
 *
 * 對帳以相同 immutable 輸入重放 P20 gate，再比較既有紀錄；它只回報差異，不會
 * 寫入資料、重新執行交易，或把 mismatch 自動修正為重放結果。
 */
public final class FuturesSandboxContractTradingReconciliationCheck {
    private final FuturesSandboxContractTradingFlowGate flowGate = new FuturesSandboxContractTradingFlowGate();

    /**
     * 重放 flow gate 並與既有紀錄比對。
     *
     * 以同一個 gate 重放避免對帳邏輯另行複製決策規則而漂移；回傳的 mismatch 仍
     * 必須由人類 review，不能被當成對既有紀錄的覆寫授權。
     */
    public FuturesSandboxContractTradingReconciliationResult check(
            FuturesSandboxContractEligibilityResult contractResult,
            FuturesSandboxLiquidationSimulationResult liquidationResult,
            FuturesSandboxContractTradingFlowResult recordedFlow
    ) {
        Objects.requireNonNull(contractResult, "contractResult must not be null");
        Objects.requireNonNull(liquidationResult, "liquidationResult must not be null");
        Objects.requireNonNull(recordedFlow, "recordedFlow must not be null");
        FuturesSandboxContractTradingFlowResult replayedFlow = flowGate.evaluate(contractResult, liquidationResult);
        if (replayedFlow.equals(recordedFlow)) {
            return FuturesSandboxContractTradingReconciliationResult.consistent(replayedFlow);
        }
        return FuturesSandboxContractTradingReconciliationResult.mismatch(replayedFlow, recordedFlow);
    }
}
