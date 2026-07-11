package com.lumix.trading.core.spot.orderbook;

import java.util.List;

/**
 * Spot sandbox order book 的安全政策。
 *
 * 這個 policy 只描述 in-memory sandbox book 的邊界，不會接 repository、transaction 或任何正式 matching runtime。
 */
public final class SpotSandboxOrderBookPolicy {

    /**
     * 建立 sandbox order book 的設計契約。
     *
     * 這份輸出只服務 phase 16 的 in-memory book gate，不代表已經可以做正式 order persistence。
     */
    public SpotSandboxOrderBookDesign describe() {
        return new SpotSandboxOrderBookDesign(
                List.of(
                        SpotSandboxOrderStatus.ACCEPTED,
                        SpotSandboxOrderStatus.OPEN,
                        SpotSandboxOrderStatus.REJECTED,
                        SpotSandboxOrderStatus.CANCELLED,
                        SpotSandboxOrderStatus.FILLED,
                        SpotSandboxOrderStatus.PARTIALLY_FILLED
                ),
                List.of(
                        "in-memory sandbox book only",
                        "只接受 P16-T02 accepted intake result 或 accepted command",
                        "accepted order book insert 不代表 persisted / reserved / matched / settled / posted",
                        "marketSymbol 查詢只回傳該 market 的 open orders",
                        "price / quantity / remainingQuantity 一律使用 BigDecimal，不得使用 float / double"
                ),
                List.of(
                        "duplicate idempotencyKey 不得建立第二筆不同 sandboxOrderId",
                        "duplicate 只能回傳 existing result 或 duplicate rejected",
                        "rejected intake result 不得進 book",
                        "本題不得產生 FILLED / PARTIALLY_FILLED"
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
     * 確認 in-memory order book 只允許 sandbox-only 範圍。
     *
     * 這個方法只回傳設計意圖，不代表任何 runtime 已完成。
     */
    public boolean requiresSandboxOnly() {
        return true;
    }

    /**
     * 確認 order book 不可被誤寫成正式交易上線。
     *
     * 這個方法只回傳設計意圖，不代表任何 runtime 已完成。
     */
    public boolean forbidsProductionReadyClaim() {
        return true;
    }

    /**
     * 確認 order book 必須是 in-memory。
     *
     * 這個方法只回傳設計意圖，不代表任何 runtime 已完成。
     */
    public boolean requiresInMemoryBook() {
        return true;
    }

    /**
     * 確認 book 只接受已通過 intake validation 的 order。
     *
     * 這個方法只回傳設計意圖，不代表任何 runtime 已完成。
     */
    public boolean requiresIntakeAcceptance() {
        return true;
    }

    /**
     * 確認 duplicate idempotencyKey 必須被保守處理。
     *
     * 這個方法只回傳設計意圖，不代表任何 runtime 已完成。
     */
    public boolean requiresDuplicateIdempotencyProtection() {
        return true;
    }

    /**
     * 確認本題不得產生 FILLED / PARTIALLY_FILLED。
     *
     * 這個方法只回傳設計意圖，不代表任何 runtime 已完成。
     */
    public boolean forbidsFilledStatusesInThisTask() {
        return true;
    }

    /**
     * 確認 book 不可直接寫入任何交易資料表。
     *
     * 這個方法只回傳設計意圖，不代表任何 runtime 已完成。
     */
    public boolean forbidsDirectPersistence() {
        return true;
    }

    /**
     * 確認 order book 不得繞過 reservation / matching / settlement / ledger / balance boundaries。
     *
     * 這個方法只回傳設計意圖，不代表任何 runtime 已完成。
     */
    public boolean forbidsBoundaryBypass() {
        return true;
    }
}
