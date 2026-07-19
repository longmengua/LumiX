package com.lumix.trading.core.futures.sandbox.integration;

import java.util.List;
import java.util.Objects;

/**
 * P20-T04 的唯讀 admin/audit review gate。
 *
 * 一致的 sandbox 結果仍需要人類確認，因為它不具備正式交易、帳務或管理權限的安全
 * 前提；mismatch 只升級調查，不會嘗試修復既有紀錄。
 */
public final class FuturesSandboxContractTradingAdminAuditReviewGate {
    private static final List<String> REQUIRED_REVIEW_ACTIONS = List.of(
            "檢查 replayedFlow 與 recordedFlow 的 decision 與 reason",
            "確認結果只代表受限 sandbox eligibility，不代表交易可執行",
            "確認沒有產生 matching、fill、position、balance、ledger 或 settlement 副作用"
    );
    private static final List<String> PROHIBITED_ACTIONS = List.of(
            "不得授權 contract trading execution",
            "不得執行 admin manual balance adjustment",
            "不得覆寫 recorded flow 或自動修正 reconciliation mismatch",
            "不得寫入 ledger、balance、reservation 或 settlement",
            "不得開放 public trading 或 real-money capability"
    );

    /**
     * 將重放對帳結論轉成唯讀 review snapshot。
     *
     * 此處不接受管理員身分或任何 command，避免呼叫端將 review 結果誤接成權限授予
     * 或資產操作入口；真正的 admin authorization 必須在後續受審核的安全邊界另行設計。
     */
    public FuturesSandboxContractTradingAdminAuditReview review(
            FuturesSandboxContractTradingReconciliationResult reconciliation
    ) {
        Objects.requireNonNull(reconciliation, "reconciliation must not be null");
        if (reconciliation.decision() == FuturesSandboxContractTradingReconciliationDecision.MISMATCH_DETECTED) {
            return new FuturesSandboxContractTradingAdminAuditReview(
                    FuturesSandboxContractTradingAdminAuditReviewDecision.MISMATCH_REQUIRES_HUMAN_INVESTIGATION,
                    FuturesSandboxContractTradingAdminAuditReviewReason.SANDBOX_FLOW_RECONCILIATION_MISMATCH,
                    reconciliation,
                    REQUIRED_REVIEW_ACTIONS,
                    PROHIBITED_ACTIONS
            );
        }
        return new FuturesSandboxContractTradingAdminAuditReview(
                FuturesSandboxContractTradingAdminAuditReviewDecision.HUMAN_REVIEW_REQUIRED,
                FuturesSandboxContractTradingAdminAuditReviewReason.SANDBOX_FLOW_RECONCILIATION_CONSISTENT,
                reconciliation,
                REQUIRED_REVIEW_ACTIONS,
                PROHIBITED_ACTIONS
        );
    }
}
