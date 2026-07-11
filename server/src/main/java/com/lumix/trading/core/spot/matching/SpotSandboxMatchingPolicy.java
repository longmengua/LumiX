package com.lumix.trading.core.spot.matching;

import java.util.List;

/**
 * Spot sandbox matching 的安全政策。
 *
 * 這個 policy 只描述 sandbox matching 的設計邊界，不會接 repository、transaction 或任何正式 trade runtime。
 */
public final class SpotSandboxMatchingPolicy {

    /**
     * 建立 Spot sandbox matching 的設計契約。
     *
     * 這份輸出只服務 phase 16 的 matching design gate，不代表已經可以做正式撮合或 trade persistence。
     */
    public SpotSandboxMatchingDesign describe() {
        return new SpotSandboxMatchingDesign(
                SpotSandboxMatchingDecision.RUNTIME_NOT_IMPLEMENTED,
                List.of(
                        SpotSandboxMatchingCapability.MARKET_PARTITION,
                        SpotSandboxMatchingCapability.PRICE_PRIORITY,
                        SpotSandboxMatchingCapability.TIME_PRIORITY,
                        SpotSandboxMatchingCapability.CROSSED_LIMIT_PRICE,
                        SpotSandboxMatchingCapability.PARTIAL_FILL_SEMANTICS,
                        SpotSandboxMatchingCapability.SETTLEMENT_INPUT_BOUNDARY
                ),
                List.of(
                        SpotSandboxTradePriceRule.MAKER_PRICE,
                        SpotSandboxTradePriceRule.HUMAN_REVIEW_REQUIRED
                ),
                List.of(
                        "matching 是 explicit sandbox process，不得由 order intake 或 order book insert 偷做",
                        "matching runtime 未完成前，order book 只保存 OPEN orders",
                        "matching 必須以 marketSymbol 分區，不同 market 不得互相撮合",
                        "BUY limit price >= SELL limit price 才可能 match",
                        "BUY side 高價優先",
                        "SELL side 低價優先",
                        "同價位依 acceptedAt 或 sequence 先後排序",
                        "matchedQuantity = min(buyRemaining, sellRemaining)",
                        "remainingQuantity 不得為負",
                        "sandbox trade price 建議使用 maker order price",
                        "maker / taker 角色未在本題 runtime 內確認前，仍屬 HUMAN_REVIEW_REQUIRED",
                        "matching result 不等於 settlement completed / ledger posted / balance updated",
                        "matching 必須產生 settlement input，但 settlement 是後續 explicit process",
                        "matching 不得直接寫 ledger、balance_projections 或 reservations",
                        "matching 不得繞過 reservation boundary"
                ),
                List.of(
                        "不新增 matching runtime",
                        "不新增 trade / fill persistence runtime",
                        "不產生 FILLED / PARTIALLY_FILLED runtime mutation",
                        "不新增 DB write path",
                        "不寫 orders / trades / reservations / balance_projections / ledger tables",
                        "不接 reservation / settlement / ledger / balance projection runtime",
                        "所有 matching / trade / settlement runtime 仍屬 HUMAN_REVIEW_REQUIRED"
                )
        );
    }

    /**
     * 確認 matching 只允許 sandbox-only 範圍。
     *
     * 這個方法只回傳設計意圖，不代表任何 runtime 已完成。
     */
    public boolean requiresExplicitSandboxMatchingProcess() {
        return true;
    }

    /**
     * 確認 matching 必須以 marketSymbol 分區。
     *
     * 這個方法只回傳設計意圖，不代表任何 runtime 已完成。
     */
    public boolean requiresMarketPartition() {
        return true;
    }

    /**
     * 確認 BUY / SELL crossed limit price 條件必須先成立。
     *
     * 這個方法只回傳設計意圖，不代表任何 runtime 已完成。
     */
    public boolean requiresCrossedLimitPrice() {
        return true;
    }

    /**
     * 確認 BUY side 需採高價優先。
     *
     * 這個方法只回傳設計意圖，不代表任何 runtime 已完成。
     */
    public boolean requiresBuyHigherPricePriority() {
        return true;
    }

    /**
     * 確認 SELL side 需採低價優先。
     *
     * 這個方法只回傳設計意圖，不代表任何 runtime 已完成。
     */
    public boolean requiresSellLowerPricePriority() {
        return true;
    }

    /**
     * 確認同價位必須以 time priority 排序。
     *
     * 這個方法只回傳設計意圖，不代表任何 runtime 已完成。
     */
    public boolean requiresTimePriority() {
        return true;
    }

    /**
     * 確認 partial fill 必須使用 min(buyRemaining, sellRemaining) 語意。
     *
     * 這個方法只回傳設計意圖，不代表任何 runtime 已完成。
     */
    public boolean requiresPartialFillSemantics() {
        return true;
    }

    /**
     * 確認 matching 必須產生 settlement input boundary。
     *
     * 這個方法只回傳設計意圖，不代表任何 runtime 已完成。
     */
    public boolean requiresSettlementInputBoundary() {
        return true;
    }

    /**
     * 確認 trade price rule 在本 gate 仍屬 HUMAN_REVIEW_REQUIRED。
     *
     * 這個方法只回傳設計意圖，不代表任何 runtime 已完成。
     */
    public boolean requiresHumanReviewForTradePriceRule() {
        return true;
    }

    /**
     * 確認 matching 不能宣稱已完成 settlement / ledger posted / balance updated。
     *
     * 這個方法只回傳設計意圖，不代表任何 runtime 已完成。
     */
    public boolean forbidsRuntimeCompletionClaims() {
        return true;
    }

    /**
     * 確認 matching 不能把 trade / fill 當成已持久化 runtime。
     *
     * 這個方法只回傳設計意圖，不代表任何 runtime 已完成。
     */
    public boolean forbidsTradePersistenceRuntime() {
        return true;
    }

    /**
     * 確認 matching 不得改寫 order book 狀態。
     *
     * 這個方法只回傳設計意圖，不代表任何 runtime 已完成。
     */
    public boolean forbidsOrderBookStateMutation() {
        return true;
    }
}
