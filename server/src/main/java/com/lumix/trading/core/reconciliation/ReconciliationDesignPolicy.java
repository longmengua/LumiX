package com.lumix.trading.core.reconciliation;

import java.util.List;

/**
 * reconciliation 的設計政策。
 *
 * 這個 policy 只整理對帳邊界，不會連到 repository、transaction 或任何正式對帳 runtime。
 */
public final class ReconciliationDesignPolicy {

    /**
     * 建立 reconciliation 的設計契約。
     *
     * 這份輸出只描述未來對帳流程要比對的訊號與邊界，不代表已經可以自動修復資料。
     */
    public ReconciliationDesign describe() {
        return new ReconciliationDesign(
                ReconciliationDesignDecision.DESIGN_ONLY,
                List.of(
                        ReconciliationSignalType.LEDGER_BALANCE_MISMATCH,
                        ReconciliationSignalType.RESERVATION_LOCK_MISMATCH,
                        ReconciliationSignalType.SETTLEMENT_EXPECTATION_MISMATCH,
                        ReconciliationSignalType.PROJECTION_LAG_EXCEEDED
                ),
                List.of(
                        "ledger 是 source of truth",
                        "balance_projections 是 read model，必須能從 ledger rebuild / replay",
                        "reservation 是 hold/release 狀態模型，不是 ledger entry 替代品",
                        "settlement 是 explicit process，不能由 matching / order runtime 偷寫 ledger"
                ),
                List.of(
                        "reconciliation 必須比較 ledger_entries derived totals、balance_projections rows、reservation locked amounts、settlement expected movements",
                        "mismatch 必須產生 review / incident / repair flow，不得自動靜默修正",
                        "reconciliation 必須可追蹤 requestId，但 requestId 不是 idempotency guarantee",
                        "未來 reconciliation runtime 必須有 audit / outbox / idempotency 邊界"
                ),
                List.of(
                        "不新增正式 reconciliation runtime",
                        "不更新 balance_projections",
                        "不接 reservation runtime",
                        "不接 settlement runtime",
                        "不接 order / matching / futures / liquidation / withdrawal runtime",
                        "所有 reconciliation runtime 都屬於 HUMAN_REVIEW_REQUIRED"
                )
        );
    }

    /**
     * 確認對帳必須把 ledger 當成 source of truth。
     *
     * 這個方法只回傳設計意圖，不代表任何 runtime 已完成。
     */
    public boolean requiresLedgerAsSourceOfTruth() {
        return true;
    }

    /**
     * 確認 balance projection 必須被視為 read model。
     *
     * 這個方法只回傳設計意圖，不代表任何 runtime 已完成。
     */
    public boolean requiresBalanceProjectionAsReadModel() {
        return true;
    }

    /**
     * 確認對帳必須檢查 reservation locked amount。
     *
     * 這個方法只回傳設計意圖，不代表任何 runtime 已完成。
     */
    public boolean requiresReservationLockedAmountCheck() {
        return true;
    }

    /**
     * 確認對帳必須檢查 settlement 預期動作。
     *
     * 這個方法只回傳設計意圖，不代表任何 runtime 已完成。
     */
    public boolean requiresSettlementExpectationCheck() {
        return true;
    }

    /**
     * 確認對帳不得靜默自動修正 mismatch。
     *
     * 這個方法只回傳設計意圖，不代表任何 runtime 已完成。
     */
    public boolean forbidsSilentAutoRepair() {
        return true;
    }

    /**
     * 確認 mismatch 必須升級給人工審核。
     *
     * 這個方法只回傳設計意圖，不代表任何 runtime 已完成。
     */
    public boolean requiresHumanReviewForMismatch() {
        return true;
    }
}
