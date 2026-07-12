package com.lumix.trading.core.spot.settlement;

/**
 * Spot sandbox settlement 設計能力清單。
 *
 * 這些能力只表示未來要被設計 gate 鎖住的邊界，不代表正式 settlement runtime 已完成。
 */
public enum SpotSandboxSettlementCapability {
    /**
     * 驗證 settlement input 的邊界。
     */
    SETTLEMENT_INPUT_VALIDATION,

    /**
     * 驗證 reservation 狀態的邊界。
     */
    RESERVATION_STATE_CHECK,

    /**
     * 定義 base asset 的移動語意。
     */
    BASE_ASSET_MOVEMENT,

    /**
     * 定義 quote asset 的移動語意。
     */
    QUOTE_ASSET_MOVEMENT,

    /**
     * 定義 reservation commit / release 的設計邊界。
     */
    RESERVATION_COMMIT_RELEASE_BOUNDARY,

    /**
     * 定義 ledger posting controlled gate 的設計邊界。
     */
    LEDGER_POSTING_GATE,

    /**
     * 定義 balance projection refresh 的設計邊界。
     */
    BALANCE_PROJECTION_REFRESH_GATE,

    /**
     * 定義 reconciliation check 的設計邊界。
     */
    RECONCILIATION_CHECK,

    /**
     * 定義 outbox / audit 的設計邊界。
     */
    OUTBOX_AUDIT_BOUNDARY
}
