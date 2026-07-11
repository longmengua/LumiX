package com.lumix.trading.core.spot;

import java.util.List;

/**
 * Spot Trading Sandbox 的安全政策。
 *
 * 這個 policy 只描述 sandbox-only 的 scope 與 boundary，不會接 repository、transaction 或任何正式交易 runtime。
 */
public final class SpotSandboxBoundaryPolicy {

    /**
     * 建立 Spot sandbox 的設計契約。
     *
     * 這份輸出只服務 phase 16 的 scope gate，不代表已經可以讓 public user 做正式交易。
     */
    public SpotSandboxBoundaryDesign describe() {
        return new SpotSandboxBoundaryDesign(
                SpotSandboxRuntimeStatus.SANDBOX_BOUNDARY_DEFINED,
                List.of(
                        SpotSandboxCapability.SCOPE_GATE,
                        SpotSandboxCapability.ORDER_INTAKE_BOUNDARY,
                        SpotSandboxCapability.RESERVATION_BOUNDARY,
                        SpotSandboxCapability.MATCHING_BOUNDARY,
                        SpotSandboxCapability.SETTLEMENT_BOUNDARY,
                        SpotSandboxCapability.LEDGER_POSTING_GATE,
                        SpotSandboxCapability.BALANCE_PROJECTION_REBUILD_GATE,
                        SpotSandboxCapability.RECONCILIATION_BOUNDARY
                ),
                List.of(
                        "sandbox only",
                        "not production-ready",
                        "not public user trading ready",
                        "no real money",
                        "no external market connectivity",
                        "no withdrawal",
                        "no futures / margin / liquidation",
                        "sandbox trade result 不等於 production trade result",
                        "amount / price / quantity 一律使用 BigDecimal，不得使用 float / double"
                ),
                List.of(
                        "spot sandbox 可以在後續任務中串接 sandbox order intake",
                        "spot sandbox 可以在後續任務中串接 sandbox reservation hold/release",
                        "spot sandbox 可以在後續任務中串接 sandbox matching",
                        "spot sandbox 可以在後續任務中串接 sandbox settlement",
                        "spot sandbox 可以在後續任務中串接 ledger posting controlled gate",
                        "spot sandbox 可以在後續任務中串接 balance projection rebuild gate",
                        "spot sandbox 可以在後續任務中串接 reconciliation boundary",
                        "order intake 不得直接寫 ledger 或 balance_projections",
                        "matching 不得直接寫 ledger、balance_projections 或 reservations",
                        "settlement 必須是 explicit process，並經 ledger posting gate",
                        "reservation runtime 未完成前，不得宣稱 available / locked trading-ready",
                        "requestId 不是 idempotency guarantee",
                        "idempotency key 才是 duplicate prevention contract"
                ),
                List.of(
                        "不新增正式 order placement runtime",
                        "不新增 matching runtime",
                        "不新增 settlement runtime",
                        "不新增 reservation runtime",
                        "不新增外部市場連線",
                        "不新增真實資金流",
                        "不新增 withdrawal runtime",
                        "不新增 futures / margin / liquidation runtime",
                        "所有 money movement / settlement / reservation runtime 都屬於 HUMAN_REVIEW_REQUIRED"
                )
        );
    }

    /**
     * 確認 spot sandbox 只允許 sandbox-only 範圍。
     *
     * 這個方法只回傳設計意圖，不代表任何 runtime 已完成。
     */
    public boolean requiresSandboxOnly() {
        return true;
    }

    /**
     * 確認 spot sandbox 不可被誤寫成正式交易上線。
     *
     * 這個方法只回傳設計意圖，不代表任何 runtime 已完成。
     */
    public boolean forbidsProductionReadyClaim() {
        return true;
    }

    /**
     * 確認 order intake boundary 必須存在。
     *
     * 這個方法只回傳設計意圖，不代表任何 runtime 已完成。
     */
    public boolean requiresOrderIntakeBoundary() {
        return true;
    }

    /**
     * 確認 reservation boundary 必須存在。
     *
     * 這個方法只回傳設計意圖，不代表任何 runtime 已完成。
     */
    public boolean requiresReservationBoundary() {
        return true;
    }

    /**
     * 確認 matching boundary 必須存在。
     *
     * 這個方法只回傳設計意圖，不代表任何 runtime 已完成。
     */
    public boolean requiresMatchingBoundary() {
        return true;
    }

    /**
     * 確認 settlement boundary 必須存在。
     *
     * 這個方法只回傳設計意圖，不代表任何 runtime 已完成。
     */
    public boolean requiresSettlementBoundary() {
        return true;
    }

    /**
     * 確認 ledger posting gate 必須存在。
     *
     * 這個方法只回傳設計意圖，不代表任何 runtime 已完成。
     */
    public boolean requiresLedgerPostingGate() {
        return true;
    }

    /**
     * 確認 balance projection rebuild gate 必須存在。
     *
     * 這個方法只回傳設計意圖，不代表任何 runtime 已完成。
     */
    public boolean requiresBalanceProjectionRebuildGate() {
        return true;
    }

    /**
     * 確認 reconciliation boundary 必須存在。
     *
     * 這個方法只回傳設計意圖，不代表任何 runtime 已完成。
     */
    public boolean requiresReconciliationBoundary() {
        return true;
    }

    /**
     * 確認 order / matching 不得直接寫 ledger。
     *
     * 這個方法只回傳設計意圖，不代表任何 runtime 已完成。
     */
    public boolean forbidsDirectLedgerWriteFromOrderOrMatching() {
        return true;
    }

    /**
     * 確認 order / matching 不得直接寫 balance_projections。
     *
     * 這個方法只回傳設計意圖，不代表任何 runtime 已完成。
     */
    public boolean forbidsDirectBalanceProjectionWriteFromOrderOrMatching() {
        return true;
    }

    /**
     * 確認 trading amount / price / quantity 都要用 BigDecimal。
     *
     * 這個方法只回傳設計意圖，不代表任何 runtime 已完成。
     */
    public boolean requiresBigDecimalForTradingAmounts() {
        return true;
    }
}
