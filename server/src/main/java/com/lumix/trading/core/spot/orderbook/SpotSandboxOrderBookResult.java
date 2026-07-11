package com.lumix.trading.core.spot.orderbook;

/**
 * Spot sandbox order book 的結果。
 *
 * accepted / duplicate / rejected 只表示 sandbox book 的受理狀態，不代表 persisted、reserved、matched、settled 或 posted。
 */
public record SpotSandboxOrderBookResult(
        SpotSandboxOrderBookDecision decision,
        SpotSandboxOrderRecord record,
        SpotSandboxOrderBookRejectionReason rejectionReason,
        String message
) {

    /**
     * 建立 accepted result。
     *
     * 這裡只表示 in-memory book 收下該筆 record，不代表任何交易 runtime 已完成。
     */
    public static SpotSandboxOrderBookResult accepted(SpotSandboxOrderRecord record) {
        return new SpotSandboxOrderBookResult(SpotSandboxOrderBookDecision.ACCEPTED, record, null, null);
    }

    /**
     * 建立 duplicate result。
     *
     * 這裡會回傳既有 record，避免同一個 idempotencyKey 產生第二筆不同 sandboxOrderId。
     */
    public static SpotSandboxOrderBookResult duplicate(SpotSandboxOrderRecord record) {
        return new SpotSandboxOrderBookResult(SpotSandboxOrderBookDecision.DUPLICATE, record, SpotSandboxOrderBookRejectionReason.DUPLICATE_IDEMPOTENCY_KEY, null);
    }

    /**
     * 建立 rejected result。
     *
     * 這裡只保存安全的拒絕原因與 message，不包含任何 SQL、stack trace 或敏感資訊。
     */
    public static SpotSandboxOrderBookResult rejected(SpotSandboxOrderBookRejectionReason reason, String message) {
        return new SpotSandboxOrderBookResult(SpotSandboxOrderBookDecision.REJECTED, null, reason, message);
    }
}
