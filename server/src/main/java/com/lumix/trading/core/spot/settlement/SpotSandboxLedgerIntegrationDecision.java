package com.lumix.trading.core.spot.settlement;

/**
 * Spot sandbox settlement 與 ledger integration 的設計決策狀態。
 *
 * 這個 enum 只描述 design gate，不代表正式 ledger posting integration runtime 已完成。
 */
public enum SpotSandboxLedgerIntegrationDecision {
    /**
     * 只停在設計階段。
     */
    DESIGN_ONLY,

    /**
     * integration runtime 尚未實作。
     */
    RUNTIME_NOT_IMPLEMENTED,

    /**
     * 任何 ledger / money movement 接線都必須經人工審核。
     */
    HUMAN_REVIEW_REQUIRED
}
