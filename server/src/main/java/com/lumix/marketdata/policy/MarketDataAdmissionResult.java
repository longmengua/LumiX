package com.lumix.marketdata.policy;

import java.util.Objects;

/**
 * 單次 admission 的 immutable 結果。
 *
 * <p>{@link #shouldApplyEvent()} 是未來 projection 唯一可依據的套用訊號；即使 cursor 因 stale 或
 * resync 狀態而更新，非 {@code ACCEPTED} 決策也絕不可使 event 被重複或越序套用。</p>
 */
public record MarketDataAdmissionResult(
        MarketDataAdmissionDecision decision,
        MarketDataAdmissionReason reason,
        MarketDataStreamCursor nextCursor
) {

    public MarketDataAdmissionResult {
        decision = Objects.requireNonNull(decision, "decision must not be null");
        reason = Objects.requireNonNull(reason, "reason must not be null");
        nextCursor = Objects.requireNonNull(nextCursor, "nextCursor must not be null");
    }

    /**
     * 僅 ACCEPTED 可進入後續唯讀 projection；health 必須由下游另外檢查，不能以 accepted 推論為即時健康。
     */
    public boolean shouldApplyEvent() {
        return decision == MarketDataAdmissionDecision.ACCEPTED;
    }
}
