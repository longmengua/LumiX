package com.lumix.trading.core.spot.matching;

/**
 * Spot sandbox matching 的設計邊界入口。
 *
 * 這個 boundary 只回傳 matching 設計契約，不代表正式撮合 runtime、trade persistence 或 settlement 已完成。
 */
public final class SpotSandboxMatchingBoundary {

    private final SpotSandboxMatchingPolicy policy = new SpotSandboxMatchingPolicy();

    /**
     * 取得 sandbox matching 設計契約。
     *
     * 這個方法只描述設計狀態，不代表任何 runtime 已完成。
     */
    public SpotSandboxMatchingDesign describe() {
        return policy.describe();
    }

    /**
     * 確認 matching 只允許 sandbox-only 範圍。
     *
     * 這個方法只回傳設計意圖，不代表任何 runtime 已完成。
     */
    public boolean requiresExplicitSandboxMatchingProcess() {
        return policy.requiresExplicitSandboxMatchingProcess();
    }

    /**
     * 確認 matching 必須以 marketSymbol 分區。
     *
     * 這個方法只回傳設計意圖，不代表任何 runtime 已完成。
     */
    public boolean requiresMarketPartition() {
        return policy.requiresMarketPartition();
    }

    /**
     * 確認 BUY / SELL crossed limit price 條件必須先成立。
     *
     * 這個方法只回傳設計意圖，不代表任何 runtime 已完成。
     */
    public boolean requiresCrossedLimitPrice() {
        return policy.requiresCrossedLimitPrice();
    }

    /**
     * 確認 BUY side 需採高價優先。
     *
     * 這個方法只回傳設計意圖，不代表任何 runtime 已完成。
     */
    public boolean requiresBuyHigherPricePriority() {
        return policy.requiresBuyHigherPricePriority();
    }

    /**
     * 確認 SELL side 需採低價優先。
     *
     * 這個方法只回傳設計意圖，不代表任何 runtime 已完成。
     */
    public boolean requiresSellLowerPricePriority() {
        return policy.requiresSellLowerPricePriority();
    }

    /**
     * 確認同價位必須以 time priority 排序。
     *
     * 這個方法只回傳設計意圖，不代表任何 runtime 已完成。
     */
    public boolean requiresTimePriority() {
        return policy.requiresTimePriority();
    }

    /**
     * 確認 partial fill 必須使用 min(buyRemaining, sellRemaining) 語意。
     *
     * 這個方法只回傳設計意圖，不代表任何 runtime 已完成。
     */
    public boolean requiresPartialFillSemantics() {
        return policy.requiresPartialFillSemantics();
    }

    /**
     * 確認 matching 必須產生 settlement input boundary。
     *
     * 這個方法只回傳設計意圖，不代表任何 runtime 已完成。
     */
    public boolean requiresSettlementInputBoundary() {
        return policy.requiresSettlementInputBoundary();
    }

    /**
     * 確認 trade price rule 在本 gate 仍屬 HUMAN_REVIEW_REQUIRED。
     *
     * 這個方法只回傳設計意圖，不代表任何 runtime 已完成。
     */
    public boolean requiresHumanReviewForTradePriceRule() {
        return policy.requiresHumanReviewForTradePriceRule();
    }

    /**
     * 確認 matching 不能宣稱已完成 settlement / ledger posted / balance updated。
     *
     * 這個方法只回傳設計意圖，不代表任何 runtime 已完成。
     */
    public boolean forbidsRuntimeCompletionClaims() {
        return policy.forbidsRuntimeCompletionClaims();
    }

    /**
     * 確認 matching 不能把 trade / fill 當成已持久化 runtime。
     *
     * 這個方法只回傳設計意圖，不代表任何 runtime 已完成。
     */
    public boolean forbidsTradePersistenceRuntime() {
        return policy.forbidsTradePersistenceRuntime();
    }

    /**
     * 確認 matching 不得改寫 order book 狀態。
     *
     * 這個方法只回傳設計意圖，不代表任何 runtime 已完成。
     */
    public boolean forbidsOrderBookStateMutation() {
        return policy.forbidsOrderBookStateMutation();
    }
}
