package com.lumix.trading.core.futures.sandbox.liquidation;

/**
 * 單次 liquidation simulation 的結果類型。
 *
 * LIQUIDATION_SIMULATED 只代表明確輸入落入 simulation 門檻，絕不會發出強平交易或改變任何領域狀態。
 */
public enum FuturesSandboxLiquidationSimulationDecision {
    NOT_LIQUIDATABLE,
    LIQUIDATION_SIMULATED
}
