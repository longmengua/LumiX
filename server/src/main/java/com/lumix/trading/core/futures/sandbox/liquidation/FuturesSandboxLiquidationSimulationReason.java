package com.lumix.trading.core.futures.sandbox.liquidation;

/**
 * 單次 liquidation simulation 的可稽核結論原因。
 */
public enum FuturesSandboxLiquidationSimulationReason {
    EQUITY_ABOVE_MAINTENANCE_MARGIN,
    EQUITY_AT_OR_BELOW_MAINTENANCE_MARGIN
}
