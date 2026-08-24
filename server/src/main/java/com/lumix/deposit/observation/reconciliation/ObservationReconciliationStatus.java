package com.lumix.deposit.observation.reconciliation;

/**
 * 唯讀 reconciliation 的總結狀態；除了 RECONCILED 外一律不得交付 P23 候選。
 */
public enum ObservationReconciliationStatus {
    RECONCILED,
    BLOCKED_NETWORK_HEALTH,
    MISSING_EVIDENCE,
    CONFLICTING_EVIDENCE
}
