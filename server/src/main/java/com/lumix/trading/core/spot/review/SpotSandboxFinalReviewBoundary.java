package com.lumix.trading.core.spot.review;

import java.util.List;

/**
 * 提供 Phase 16 spot sandbox final review 的固定狀態與審查語意，避免後續文件把 foundation 誤寫成正式交易完成。
 */
public final class SpotSandboxFinalReviewBoundary {

    private static final List<String> COMPLETED_ITEMS = List.of(
            "P16-T01 spot sandbox scope gate and runtime boundaries",
            "P16-T02 spot sandbox order intake boundary",
            "P16-T03 spot sandbox in-memory order book gate",
            "P16-T04 spot sandbox matching design gate",
            "P16-T05 spot sandbox in-memory matching runtime",
            "P16-T06 spot sandbox trade/fill result boundary",
            "P16-T07 spot sandbox settlement design gate",
            "P16-T08 spot sandbox settlement runtime gate",
            "P16-T09 spot sandbox ledger posting integration design gate",
            "P16-T10 phase 16 final review gate"
    );

    private static final List<String> NOT_COMPLETED_ITEMS = List.of(
            "DB order persistence",
            "DB trade persistence",
            "reservation runtime",
            "actual ledger posting integration",
            "balance projection refresh integration",
            "reconciliation runtime",
            "idempotency store / lookup",
            "outbox / audit runtime",
            "production security / ops / monitoring",
            "public user trading",
            "real money movement",
            "withdrawal",
            "futures / margin / liquidation"
    );

    private static final List<String> FORBIDDEN_CLAIMS = List.of(
            "production-ready",
            "exchange ready",
            "public trading ready",
            "real-money ready",
            "ledger posted",
            "balance updated",
            "reservation committed",
            "settlement finalized",
            "full trading runtime completed",
            "spot trading production ready"
    );

    /**
     * 建立 Phase 16 final review 的固定邊界物件。
     */
    public SpotSandboxFinalReviewBoundary() {
    }

    /**
     * 回傳 Phase 16 必須使用的最終狀態字串，避免文件把 sandbox foundation 寫成尚未收斂的進行中狀態。
     *
     * @return Phase 16 的最終狀態語意。
     */
    public String phase16Status() {
        return "Phase 16: COMPLETED_FOR_SPOT_SANDBOX_FOUNDATION";
    }

    /**
     * 回傳 Phase 16 已完成的 sandbox foundation 說明，供 final review 文件與測試對照。
     *
     * @return 已完成的 Phase 16 工作清單。
     */
    public List<String> completedItems() {
        return COMPLETED_ITEMS;
    }

    /**
     * 回傳 Phase 16 尚未完成的 runtime 範圍，避免 review gate 誤把 sandbox foundation 視為正式交易完成。
     *
     * @return 尚未完成的 runtime 清單。
     */
    public List<String> notCompletedItems() {
        return NOT_COMPLETED_ITEMS;
    }

    /**
     * 回傳 Phase 16 必須避免的誤寫宣稱，讓 final review 可以直接對照禁止語句。
     *
     * @return 不得出現在 final review 的宣稱清單。
     */
    public List<String> forbiddenClaims() {
        return FORBIDDEN_CLAIMS;
    }

    /**
     * 標示 Phase 16 仍然需要人類審查，不能被解讀成正式交易就緒。
     *
     * @return 永遠回傳 `true`，代表仍屬 HUMAN_REVIEW_REQUIRED。
     */
    public boolean requiresHumanReviewForMoneyMovement() {
        return true;
    }
}
