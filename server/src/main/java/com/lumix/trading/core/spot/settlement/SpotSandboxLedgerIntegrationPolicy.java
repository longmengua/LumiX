package com.lumix.trading.core.spot.settlement;

import java.util.List;

/**
 * Spot sandbox settlement 與 ledger integration 的安全政策。
 *
 * 這個 policy 只描述 design gate，不會接 repository、transaction 或任何正式 ledger posting runtime。
 */
public final class SpotSandboxLedgerIntegrationPolicy {

    /**
     * 建立 Spot sandbox settlement 與 ledger integration 的設計契約。
     *
     * 這份輸出只服務 phase 16 的 integration design gate，不代表已經可以把 settlement plan 接進正式 ledger runtime。
     */
    public SpotSandboxLedgerIntegrationDesign describe() {
        return new SpotSandboxLedgerIntegrationDesign(
                SpotSandboxLedgerIntegrationDecision.DESIGN_ONLY,
                List.of(
                        SpotSandboxLedgerIntegrationStep.IDEMPOTENCY_DECISION,
                        SpotSandboxLedgerIntegrationStep.RESERVATION_STATE_VERIFICATION,
                        SpotSandboxLedgerIntegrationStep.SETTLEMENT_INPUT_VALIDATION,
                        SpotSandboxLedgerIntegrationStep.LEDGER_CANDIDATE_INVARIANT_CHECK,
                        SpotSandboxLedgerIntegrationStep.HUMAN_REVIEW_GATE,
                        SpotSandboxLedgerIntegrationStep.LEDGER_POSTING_CONTROLLED_GATE,
                        SpotSandboxLedgerIntegrationStep.BALANCE_PROJECTION_REFRESH_GATE,
                        SpotSandboxLedgerIntegrationStep.RECONCILIATION_CHECK,
                        SpotSandboxLedgerIntegrationStep.OUTBOX_AUDIT_BOUNDARY
                ),
                List.of(
                        SpotSandboxLedgerIntegrationRisk.DUPLICATE_SETTLEMENT,
                        SpotSandboxLedgerIntegrationRisk.LEDGER_APPEND_FAILURE,
                        SpotSandboxLedgerIntegrationRisk.BALANCE_REFRESH_LAG,
                        SpotSandboxLedgerIntegrationRisk.RESERVATION_COMMIT_MISMATCH,
                        SpotSandboxLedgerIntegrationRisk.RECONCILIATION_MISMATCH
                ),
                List.of(
                        "settlement plan ledger candidate 只是 candidate，不是 posted journal",
                        "接 LedgerPostingRuntimeGate 前必須完成 idempotency decision",
                        "接 LedgerPostingRuntimeGate 前必須完成 reservation state verification",
                        "接 LedgerPostingRuntimeGate 前必須完成 settlement input validation",
                        "接 LedgerPostingRuntimeGate 前必須完成 ledger candidate invariant check",
                        "接 LedgerPostingRuntimeGate 前必須完成 human review gate",
                        "ledger posting controlled gate 成功後，才可以標示 ledger append completed",
                        "ledger append completed 不等於 balance projection refreshed",
                        "ledger append completed 不等於 reservation committed",
                        "ledger append completed 不等於 settlement fully completed",
                        "ledger append completed 不等於 reconciliation completed",
                        "duplicate settlement 必須由 idempotency key 控制，不得靠 requestId",
                        "requestId 只做 trace / correlation / audit linkage",
                        "四腿 ledger candidate 必須保留 buyer base CREDIT quantity / buyer quote DEBIT quoteAmount / seller base DEBIT quantity / seller quote CREDIT quoteAmount",
                        "outbox / audit 必須是後續 explicit boundary，不得偷寫"
                ),
                List.of(
                        "不新增 ledger posting integration runtime",
                        "不呼叫 LedgerPostingRuntimeGate",
                        "不呼叫 LedgerAppendOnlyJdbcAdapter",
                        "不呼叫 BalanceProjectionRebuildGate",
                        "不寫 ledger_journals / ledger_entries",
                        "不更新 balance_projections",
                        "不更新 reservations",
                        "不接 idempotency_keys / outbox_events / audit_logs runtime",
                        "不宣稱 production-ready",
                        "不宣稱 ledger posted",
                        "不宣稱 settlement completed",
                        "不宣稱 balance updated",
                        "不宣稱 reservation committed"
                )
        );
    }

    /**
     * 確認 integration 只允許設計階段，不會被誤寫成正式 runtime。
     *
     * 這個方法只回傳設計意圖，不代表任何 runtime 已完成。
     */
    public boolean requiresDesignOnly() {
        return true;
    }

    /**
     * 確認接正式 ledger runtime 前必須先完成 idempotency decision。
     *
     * 這個方法只回傳設計意圖，不代表任何 runtime 已完成。
     */
    public boolean requiresIdempotencyDecisionBeforeLedgerRuntime() {
        return true;
    }

    /**
     * 確認接正式 ledger runtime 前必須先完成 reservation state verification。
     *
     * 這個方法只回傳設計意圖，不代表任何 runtime 已完成。
     */
    public boolean requiresReservationStateVerificationBeforeLedgerRuntime() {
        return true;
    }

    /**
     * 確認接正式 ledger runtime 前必須先完成 settlement input validation。
     *
     * 這個方法只回傳設計意圖，不代表任何 runtime 已完成。
     */
    public boolean requiresSettlementInputValidationBeforeLedgerRuntime() {
        return true;
    }

    /**
     * 確認接正式 ledger runtime 前必須先完成 ledger candidate invariant check。
     *
     * 這個方法只回傳設計意圖，不代表任何 runtime 已完成。
     */
    public boolean requiresLedgerCandidateInvariantCheckBeforeLedgerRuntime() {
        return true;
    }

    /**
     * 確認接正式 ledger runtime 前必須先通過 human review gate。
     *
     * 這個方法只回傳設計意圖，不代表任何 runtime 已完成。
     */
    public boolean requiresHumanReviewGateBeforeLedgerRuntime() {
        return true;
    }

    /**
     * 確認 ledger append completed 不等於 balance refresh / reservation commit / reconciliation completed。
     *
     * 這個方法只回傳設計意圖，不代表任何 runtime 已完成。
     */
    public boolean forbidsCompletionClaimsBeyondLedgerAppend() {
        return true;
    }

    /**
     * 確認 requestId 只做 trace / correlation / audit linkage。
     *
     * 這個方法只回傳設計意圖，不代表任何 runtime 已完成。
     */
    public boolean requiresRequestIdOnlyForTraceAndAuditLinkage() {
        return true;
    }

    /**
     * 確認 duplicate settlement 只能由 idempotency key 控制。
     *
     * 這個方法只回傳設計意圖，不代表任何 runtime 已完成。
     */
    public boolean requiresIdempotencyKeyForDuplicateSettlementControl() {
        return true;
    }

    /**
     * 確認 outbox / audit 只能做後續 explicit boundary。
     *
     * 這個方法只回傳設計意圖，不代表任何 runtime 已完成。
     */
    public boolean requiresOutboxAuditAsExplicitBoundary() {
        return true;
    }
}
