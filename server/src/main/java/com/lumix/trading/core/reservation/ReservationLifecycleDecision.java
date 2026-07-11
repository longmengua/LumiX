package com.lumix.trading.core.reservation;

/**
 * reservation 設計階段的 lifecycle 決策。
 *
 * 這些值只表示目前是在設計門檻、正式 runtime 需求，或高風險人工審核狀態，不代表 runtime 已完成。
 */
public enum ReservationLifecycleDecision {
    DESIGN_ONLY,
    RUNTIME_REQUIRED,
    HUMAN_REVIEW_REQUIRED
}
