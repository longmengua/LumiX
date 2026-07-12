package com.lumix.trading.core.spot.settlement;

/**
 * Spot sandbox settlement runtime gate 的判斷結果。
 *
 * 這個 enum 只描述 plan gate 的決策，不代表正式 settlement runtime、ledger posting 或 balance refresh 已完成。
 */
public enum SpotSandboxSettlementRuntimeDecision {
    /**
     * 已建立 sandbox plan，但尚未進入任何正式下游 runtime。
     */
    PLANNED,

    /**
     * 輸入不符合 sandbox settlement runtime gate。
     */
    REJECTED
}
