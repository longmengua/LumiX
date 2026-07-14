package com.lumix.trading.core.futures.position;

/**
 * Futures position 方向。
 *
 * 只允許 LONG / SHORT，避免用 signed quantity 重複表達方向，讓模型保持單一語意來源。
 */
public enum FuturesPositionSide {
    LONG,
    SHORT;

    /**
     * 回傳是否為 LONG。
     *
     * 這只是語意 helper，不是額外的 direction 狀態來源。
     */
    public boolean isLong() {
        return this == LONG;
    }

    /**
     * 回傳是否為 SHORT。
     *
     * 這只是語意 helper，不是額外的 direction 狀態來源。
     */
    public boolean isShort() {
        return this == SHORT;
    }
}
