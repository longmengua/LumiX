package com.lumix.trading.core.futures.sandbox.integration;

/** P20-T03 重放對帳的結論；只指出是否一致，不會觸發任何修正。 */
public enum FuturesSandboxContractTradingReconciliationDecision {
    CONSISTENT,
    MISMATCH_DETECTED
}
