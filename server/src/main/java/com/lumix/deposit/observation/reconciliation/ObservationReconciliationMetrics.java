package com.lumix.deposit.observation.reconciliation;

/**
 * 可由 report 直接重算的唯讀觀測計數；不代表 production metrics endpoint。
 */
public record ObservationReconciliationMetrics(
        int observedCount,
        int pendingConfirmationCount,
        int finalityThresholdMetCount,
        int orphanedCount,
        int quarantinedCount
) {

    public ObservationReconciliationMetrics {
        if (observedCount < 0 || pendingConfirmationCount < 0 || finalityThresholdMetCount < 0
                || orphanedCount < 0 || quarantinedCount < 0) {
            throw new IllegalArgumentException("reconciliation metrics must not be negative");
        }
    }
}
