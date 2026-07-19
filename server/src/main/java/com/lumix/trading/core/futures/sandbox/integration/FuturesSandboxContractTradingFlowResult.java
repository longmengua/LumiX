package com.lumix.trading.core.futures.sandbox.integration;

/** P20-T01 gate 的 immutable 結果。 */
public record FuturesSandboxContractTradingFlowResult(FuturesSandboxContractTradingFlowDecision decision, FuturesSandboxContractTradingFlowReason reason) {
    public static FuturesSandboxContractTradingFlowResult eligible() { return new FuturesSandboxContractTradingFlowResult(FuturesSandboxContractTradingFlowDecision.ELIGIBLE_FOR_SANDBOX_FLOW, FuturesSandboxContractTradingFlowReason.CONTRACT_AND_RISK_SIMULATION_ACCEPTED); }
    public static FuturesSandboxContractTradingFlowResult rejected(FuturesSandboxContractTradingFlowReason reason) { return new FuturesSandboxContractTradingFlowResult(FuturesSandboxContractTradingFlowDecision.REJECTED, reason); }
}
