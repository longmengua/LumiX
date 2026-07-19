package com.lumix.trading.core.futures.sandbox.contract;

/**
 * 受限 futures sandbox contract inspection 的可稽核結論原因。
 */
public enum FuturesSandboxContractEligibilityReason {
    ACCEPTED_ORDER_AND_MOCK_PRICE_WITHIN_CONTRACT,
    ORDER_NOT_ACCEPTED_FOR_SANDBOX,
    ORDER_MARKET_OUTSIDE_CONTRACT,
    MARK_PRICE_MARKET_OUTSIDE_CONTRACT
}
