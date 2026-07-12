package com.lumix.trading.core.spot.settlement;

/**
 * Spot sandbox settlement 的設計決策狀態。
 *
 * 這個 enum 只描述設計門檻，不代表 settlement runtime、ledger posting 或 balance refresh 已完成。
 */
public enum SpotSandboxSettlementDecision {
    /**
     * 只停在設計階段。
     */
    DESIGN_ONLY,

    /**
     * settlement runtime 尚未實作。
     */
    RUNTIME_NOT_IMPLEMENTED,

    /**
     * settlement 屬於高風險流程，任何正式接線都必須保留人工審核。
     */
    HUMAN_REVIEW_REQUIRED
}
