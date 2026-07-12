package com.lumix.trading.core.spot.settlement;

import java.util.List;

/**
 * Spot Trading Sandbox 的 settlement 安全政策。
 *
 * 這個 policy 只描述 settlement 設計邊界，不會接 repository、transaction 或任何正式 settlement runtime。
 */
public final class SpotSandboxSettlementPolicy {

    /**
     * 建立 Spot sandbox settlement 的設計契約。
     *
     * 這份輸出只服務 phase 16 的 settlement design gate，不代表已經可以做正式 settlement 或 ledger posting。
     */
    public SpotSandboxSettlementDesign describe() {
        return new SpotSandboxSettlementDesign(
                SpotSandboxSettlementDecision.DESIGN_ONLY,
                List.of(
                        SpotSandboxSettlementCapability.SETTLEMENT_INPUT_VALIDATION,
                        SpotSandboxSettlementCapability.RESERVATION_STATE_CHECK,
                        SpotSandboxSettlementCapability.BASE_ASSET_MOVEMENT,
                        SpotSandboxSettlementCapability.QUOTE_ASSET_MOVEMENT,
                        SpotSandboxSettlementCapability.RESERVATION_COMMIT_RELEASE_BOUNDARY,
                        SpotSandboxSettlementCapability.LEDGER_POSTING_GATE,
                        SpotSandboxSettlementCapability.BALANCE_PROJECTION_REFRESH_GATE,
                        SpotSandboxSettlementCapability.RECONCILIATION_CHECK,
                        SpotSandboxSettlementCapability.OUTBOX_AUDIT_BOUNDARY
                ),
                List.of(
                        SpotSandboxSettlementStep.IDEMPOTENCY_DECISION,
                        SpotSandboxSettlementStep.VALIDATE_SETTLEMENT_INPUT,
                        SpotSandboxSettlementStep.VERIFY_RESERVATION_STATE,
                        SpotSandboxSettlementStep.COMPUTE_ASSET_MOVEMENTS,
                        SpotSandboxSettlementStep.RESERVATION_COMMIT_RELEASE_DECISION,
                        SpotSandboxSettlementStep.LEDGER_POSTING_CONTROLLED_GATE,
                        SpotSandboxSettlementStep.BALANCE_PROJECTION_REFRESH_GATE,
                        SpotSandboxSettlementStep.RECONCILIATION_CHECK,
                        SpotSandboxSettlementStep.OUTBOX_AUDIT_BOUNDARY
                ),
                List.of(
                        "settlement 是 explicit sandbox process，不得由 matching runtime 偷做",
                        "settlement input 只能來自 P16-T06 的 sandbox trade/fill result boundary",
                        "trade/fill result 在 settlement runtime 未完成前，只能停在 SETTLEMENT_NOT_STARTED",
                        "BUY side movement：buyer receives base asset，buyer pays quote asset",
                        "SELL side movement：seller pays base asset，seller receives quote asset",
                        "quoteAmount = price * quantity",
                        "amount / price / quantity / quoteAmount 一律使用 BigDecimal，不得使用 float / double",
                        "settlement result 不等於 ledger posted / balance updated / reservation committed，除非後續 gate 明確成功",
                        "settlement mismatch 必須進 reconciliation / human review，不得靜默修正",
                        "requestId 不是 idempotency guarantee",
                        "idempotencyKey 才能防 duplicate settlement",
                        "所有 settlement runtime、reservation runtime、ledger posting integration、balance refresh、reconciliation runtime 都屬於 HUMAN_REVIEW_REQUIRED"
                ),
                List.of(
                        "不新增 settlement runtime",
                        "不更新 order book state",
                        "不更新 trade/fill state",
                        "不新增 DB write",
                        "不接 reservation runtime",
                        "不接 ledger posting runtime",
                        "不接 balance projection rebuild runtime",
                        "不接 idempotency_keys / outbox_events / audit_logs runtime",
                        "不寫 orders / trades / reservations / balance_projections / ledger tables",
                        "不宣稱 production-ready",
                        "不宣稱 settlement completed",
                        "不宣稱 ledger posted",
                        "不宣稱 balance updated",
                        "不宣稱 reservation committed"
                )
        );
    }

    /**
     * 確認 settlement 必須是 explicit process。
     *
     * 這個方法只回傳設計意圖，不代表任何 runtime 已完成。
     */
    public boolean requiresExplicitSandboxSettlementProcess() {
        return true;
    }

    /**
     * 確認 settlement input 只能來自 trade/fill boundary。
     *
     * 這個方法只回傳設計意圖，不代表任何 runtime 已完成。
     */
    public boolean requiresSettlementInputFromTradeFillBoundary() {
        return true;
    }

    /**
     * 確認 settlement runtime 必須遵守固定步驟順序。
     *
     * 這個方法只回傳設計意圖，不代表任何 runtime 已完成。
     */
    public boolean requiresSettlementStepOrder() {
        return true;
    }

    /**
     * 確認 BUY side 必須先拿 base asset，並支付 quote asset。
     *
     * 這個方法只回傳設計意圖，不代表任何 runtime 已完成。
     */
    public boolean requiresBuyerReceivesBaseAndPaysQuote() {
        return true;
    }

    /**
     * 確認 SELL side 必須先支付 base asset，並收到 quote asset。
     *
     * 這個方法只回傳設計意圖，不代表任何 runtime 已完成。
     */
    public boolean requiresSellerPaysBaseAndReceivesQuote() {
        return true;
    }

    /**
     * 確認 quoteAmount 必須由 price * quantity 推導。
     *
     * 這個方法只回傳設計意圖，不代表任何 runtime 已完成。
     */
    public boolean requiresQuoteAmountUsesPriceTimesQuantity() {
        return true;
    }

    /**
     * 確認 settlement result 不能自行宣稱 ledger posted / balance updated / reservation committed。
     *
     * 這個方法只回傳設計意圖，不代表任何 runtime 已完成。
     */
    public boolean forbidsRuntimeCompletionClaims() {
        return true;
    }

    /**
     * 確認 requestId 不等於 idempotency guarantee。
     *
     * 這個方法只回傳設計意圖，不代表任何 runtime 已完成。
     */
    public boolean requiresRequestIdNotIdempotencyGuarantee() {
        return true;
    }

    /**
     * 確認 idempotencyKey 才能防 duplicate settlement。
     *
     * 這個方法只回傳設計意圖，不代表任何 runtime 已完成。
     */
    public boolean requiresIdempotencyKeyForDuplicateSettlementPrevention() {
        return true;
    }

    /**
     * 確認 mismatch 必須進 reconciliation / human review。
     *
     * 這個方法只回傳設計意圖，不代表任何 runtime 已完成。
     */
    public boolean requiresHumanReviewForMismatch() {
        return true;
    }
}
