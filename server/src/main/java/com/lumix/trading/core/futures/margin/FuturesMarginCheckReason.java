package com.lumix.trading.core.futures.margin;

/**
 * Isolated margin sufficiency gate 的業務原因碼。
 *
 * 這些 reason 只服務 Phase 17-T04 的 sandbox gate，不代表完整 futures risk engine 或正式產品 contract。
 */
public enum FuturesMarginCheckReason {
    SUFFICIENT_MARGIN,
    INSUFFICIENT_MARGIN,
    ACCOUNT_NOT_ACTIVE,
    ACCOUNT_MISMATCH,
    MARKET_MISMATCH,
    SETTLEMENT_ASSET_MISMATCH
}
