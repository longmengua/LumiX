package com.lumix.trading.core.reconciliation;

/**
 * reconciliation 設計階段要觀察的訊號型別。
 *
 * 這些值只描述未來對帳要比對什麼，不代表已經有自動修復或寫回流程。
 */
public enum ReconciliationSignalType {
    LEDGER_BALANCE_MISMATCH,
    RESERVATION_LOCK_MISMATCH,
    SETTLEMENT_EXPECTATION_MISMATCH,
    PROJECTION_LAG_EXCEEDED
}
