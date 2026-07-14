package com.lumix.trading.core.futures.account;

/**
 * Futures account 的 margin mode。
 *
 * Phase 17-T01 只允許 ISOLATED，避免在 core model 階段偷渡 cross margin 或其他尚未審核的模式。
 */
public enum FuturesMarginMode {
    ISOLATED;

    /**
     * 回傳是否為 isolated margin。
     *
     * 目前只有一種模式，但這個 helper 讓後續 guard 可以用語意化判斷，不必依賴 magic value。
     */
    public boolean isIsolated() {
        return this == ISOLATED;
    }
}
