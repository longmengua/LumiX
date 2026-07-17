package com.lumix.trading.core.futures.order;

/**
 * Futures order 的下單方向。
 *
 * T01 的 order side 只表達 BUY / SELL 的下單意圖，刻意與 Phase 17 的 position side LONG / SHORT 分開，
 * 因為這一題不負責推導最終 position transition；BUY 不必然等同未來所有情境的開 LONG，
 * SELL 也不必然等同未來所有情境的開 SHORT。
 */
public enum FuturesOrderSide {
    BUY,
    SELL
}
