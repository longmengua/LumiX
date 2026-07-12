package com.lumix.trading.core.spot.settlement;

/**
 * Spot sandbox settlement 與 ledger integration 的對外邊界。
 *
 * 這個 boundary 只暴露 design contract，不代表正式 ledger posting integration runtime 已完成。
 */
public final class SpotSandboxLedgerIntegrationBoundary {

    private final SpotSandboxLedgerIntegrationPolicy policy;

    /**
     * 建立 sandbox ledger integration boundary。
     *
     * 這裡只包一層 policy 入口，不接任何 runtime service 或 DB client。
     */
    public SpotSandboxLedgerIntegrationBoundary() {
        this.policy = new SpotSandboxLedgerIntegrationPolicy();
    }

    /**
     * 取得 settlement 與 ledger integration 的設計契約。
     *
     * 這個方法只回傳 design gate，不代表已經可以接進正式 ledger runtime。
     */
    public SpotSandboxLedgerIntegrationDesign describe() {
        return policy.describe();
    }

    /**
     * 確認 integration 只允許設計階段。
     *
     * 這個方法只回傳設計意圖，不代表任何 runtime 已完成。
     */
    public boolean requiresDesignOnly() {
        return policy.requiresDesignOnly();
    }

    /**
     * 確認接正式 ledger runtime 前必須先完成 idempotency decision。
     *
     * 這個方法只回傳設計意圖，不代表任何 runtime 已完成。
     */
    public boolean requiresIdempotencyDecisionBeforeLedgerRuntime() {
        return policy.requiresIdempotencyDecisionBeforeLedgerRuntime();
    }
}
