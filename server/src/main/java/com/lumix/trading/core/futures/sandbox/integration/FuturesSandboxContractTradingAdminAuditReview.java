package com.lumix.trading.core.futures.sandbox.integration;

import java.util.List;
import java.util.Objects;

/**
 * P20-T04 的 immutable admin/audit review snapshot。
 *
 * snapshot 只把已完成的重放對帳結果、人工檢查項與禁止動作一起提供給審核者；它不是
 * admin command，也不攜帶帳戶、權限或任何可寫入 runtime 的能力。
 */
public record FuturesSandboxContractTradingAdminAuditReview(
        FuturesSandboxContractTradingAdminAuditReviewDecision decision,
        FuturesSandboxContractTradingAdminAuditReviewReason reason,
        FuturesSandboxContractTradingReconciliationResult reconciliation,
        List<String> requiredReviewActions,
        List<String> prohibitedActions
) {

    public FuturesSandboxContractTradingAdminAuditReview {
        Objects.requireNonNull(decision, "decision must not be null");
        Objects.requireNonNull(reason, "reason must not be null");
        Objects.requireNonNull(reconciliation, "reconciliation must not be null");
        Objects.requireNonNull(requiredReviewActions, "requiredReviewActions must not be null");
        Objects.requireNonNull(prohibitedActions, "prohibitedActions must not be null");
        requiredReviewActions = List.copyOf(requiredReviewActions);
        prohibitedActions = List.copyOf(prohibitedActions);
        if (requiredReviewActions.isEmpty() || prohibitedActions.isEmpty()) {
            throw new IllegalArgumentException("audit review must retain review actions and prohibited actions");
        }
        if (decision == FuturesSandboxContractTradingAdminAuditReviewDecision.HUMAN_REVIEW_REQUIRED
                && (reason != FuturesSandboxContractTradingAdminAuditReviewReason.SANDBOX_FLOW_RECONCILIATION_CONSISTENT
                || reconciliation.decision() != FuturesSandboxContractTradingReconciliationDecision.CONSISTENT)) {
            throw new IllegalArgumentException("consistent reconciliation must remain human-review-required");
        }
        if (decision == FuturesSandboxContractTradingAdminAuditReviewDecision.MISMATCH_REQUIRES_HUMAN_INVESTIGATION
                && (reason != FuturesSandboxContractTradingAdminAuditReviewReason.SANDBOX_FLOW_RECONCILIATION_MISMATCH
                || reconciliation.decision() != FuturesSandboxContractTradingReconciliationDecision.MISMATCH_DETECTED)) {
            throw new IllegalArgumentException("mismatch reconciliation must require human investigation");
        }
    }
}
