package com.lumix.trading.core.futures.order;

/**
 * Futures sandbox order placement 的業務結果原因。
 *
 * 這些 reason 只服務 T01 的 pure placement gate，不代表 matching、trade、position、reservation 或 settlement lifecycle。
 */
public enum FuturesOrderPlacementReason {
    SANDBOX_ORDER_ACCEPTED,
    ACCOUNT_MISMATCH,
    MARKET_MISMATCH,
    MARGIN_CHECK_NOT_APPROVED,
    MARGIN_PROPOSAL_MISMATCH
}
