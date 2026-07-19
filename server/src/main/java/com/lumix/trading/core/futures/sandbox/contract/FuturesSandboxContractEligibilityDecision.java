package com.lumix.trading.core.futures.sandbox.contract;

/**
 * 受限 futures sandbox contract inspection 的結論。
 *
 * ELIGIBLE_FOR_SANDBOX_INSPECTION 只代表輸入可在這個受限 sandbox 被檢視，
 * 不表示 order 已撮合、已成交、已開倉、已扣款或已結算。
 */
public enum FuturesSandboxContractEligibilityDecision {
    ELIGIBLE_FOR_SANDBOX_INSPECTION,
    REJECTED
}
