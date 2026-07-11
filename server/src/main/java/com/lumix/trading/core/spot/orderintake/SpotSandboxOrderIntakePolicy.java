package com.lumix.trading.core.spot.orderintake;

import java.util.List;

/**
 * Spot sandbox order intake 的安全政策。
 *
 * 這個 policy 只描述 sandbox-only 的 intake 邊界，不會接 repository、transaction 或任何正式 order placement runtime。
 */
public final class SpotSandboxOrderIntakePolicy {

    /**
     * 建立 sandbox order intake 的設計契約。
     *
     * 這份輸出只服務 phase 16 的 intake boundary，不代表 order 已經 persisted、reserved、matched 或 settled。
     */
    public SpotSandboxOrderIntakeDesign describe() {
        return new SpotSandboxOrderIntakeDesign(
                List.of(
                        "sandbox order intake boundary",
                        "in-memory validation only",
                        "requestId 只做 trace / correlation",
                        "idempotencyKey 必須存在，但本題不做 idempotency store / lookup",
                        "accepted result 不代表 persisted / reserved / matched / settled / posted",
                        "amount / price / quantity 一律使用 BigDecimal，不得使用 float / double"
                ),
                List.of(
                        "sandbox only",
                        "not production-ready",
                        "not public user trading ready",
                        "no real money",
                        "no external market connectivity",
                        "no withdrawal",
                        "no futures / margin / liquidation"
                ),
                List.of(
                        "只受理 LIMIT order",
                        "只受理 GTC time-in-force",
                        "price 必須大於 0",
                        "quantity 必須大於 0",
                        "marketSymbol 必須非空",
                        "userId 與 accountId 必須非空",
                        "BUY / SELL 語意必須清楚",
                        "MARKET 與 IOC 在本題皆視為 unsupported"
                ),
                List.of(
                        SpotSandboxOrderRejectionReason.MISSING_REQUEST_ID,
                        SpotSandboxOrderRejectionReason.MISSING_IDEMPOTENCY_KEY,
                        SpotSandboxOrderRejectionReason.MISSING_USER_ID,
                        SpotSandboxOrderRejectionReason.MISSING_ACCOUNT_ID,
                        SpotSandboxOrderRejectionReason.MISSING_MARKET_SYMBOL,
                        SpotSandboxOrderRejectionReason.MISSING_SIDE,
                        SpotSandboxOrderRejectionReason.MISSING_ORDER_TYPE,
                        SpotSandboxOrderRejectionReason.MISSING_TIME_IN_FORCE,
                        SpotSandboxOrderRejectionReason.UNSUPPORTED_ORDER_TYPE,
                        SpotSandboxOrderRejectionReason.UNSUPPORTED_TIME_IN_FORCE,
                        SpotSandboxOrderRejectionReason.INVALID_PRICE,
                        SpotSandboxOrderRejectionReason.INVALID_QUANTITY
                ),
                List.of(
                        "不新增正式 order placement runtime",
                        "不新增 matching runtime",
                        "不新增 settlement runtime",
                        "不新增 reservation runtime",
                        "不新增 DB write path",
                        "不寫 orders / trades / reservations / balance_projections / ledger tables",
                        "不接 reservation / matching / settlement / ledger / balance projection runtime",
                        "所有後續 reservation / matching / settlement 仍屬 HUMAN_REVIEW_REQUIRED"
                )
        );
    }

    /**
     * 確認 spot intake 只允許 sandbox-only 範圍。
     *
     * 這個方法只回傳設計意圖，不代表任何 runtime 已完成。
     */
    public boolean requiresSandboxOnly() {
        return true;
    }

    /**
     * 確認 spot intake 不可被誤寫成正式交易上線。
     *
     * 這個方法只回傳設計意圖，不代表任何 runtime 已完成。
     */
    public boolean forbidsProductionReadyClaim() {
        return true;
    }

    /**
     * 確認 order intake 只接受 LIMIT order。
     *
     * 這個方法只回傳設計意圖，不代表任何 runtime 已完成。
     */
    public boolean acceptsOnlyLimitOrders() {
        return true;
    }

    /**
     * 確認 order intake 只接受 GTC time-in-force。
     *
     * 這個方法只回傳設計意圖，不代表任何 runtime 已完成。
     */
    public boolean acceptsOnlyGtc() {
        return true;
    }

    /**
     * 確認 price 與 quantity 必須使用 BigDecimal。
     *
     * 這個方法只回傳設計意圖，不代表任何 runtime 已完成。
     */
    public boolean requiresBigDecimalForTradingAmounts() {
        return true;
    }

    /**
     * 確認 intake 不會直接寫入任何交易資料表。
     *
     * 這個方法只回傳設計意圖，不代表任何 runtime 已完成。
     */
    public boolean forbidsDirectPersistence() {
        return true;
    }

    /**
     * 確認 intake 不得繞過 reservation / matching / settlement / ledger / balance boundaries。
     *
     * 這個方法只回傳設計意圖，不代表任何 runtime 已完成。
     */
    public boolean forbidsBoundaryBypass() {
        return true;
    }
}
