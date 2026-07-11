package com.lumix.trading.core.reconciliation;

/**
 * reconciliation 設計階段的決策狀態。
 *
 * 這些值只表示目前仍在設計門檻，不代表對帳 runtime 已經完成。
 */
public enum ReconciliationDesignDecision {
    DESIGN_ONLY,
    HUMAN_REVIEW_REQUIRED,
    RUNTIME_NOT_IMPLEMENTED
}
