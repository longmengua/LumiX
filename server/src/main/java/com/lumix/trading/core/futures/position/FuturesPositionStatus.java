package com.lumix.trading.core.futures.position;

/**
 * Futures position 生命週期狀態。
 *
 * 目前只保留最小狀態集合，避免在 Phase 17-T02 偷渡 liquidation 或 funding 的 runtime 狀態。
 */
public enum FuturesPositionStatus {
    OPEN
}
