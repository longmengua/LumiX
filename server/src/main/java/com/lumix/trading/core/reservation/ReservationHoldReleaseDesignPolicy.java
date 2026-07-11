package com.lumix.trading.core.reservation;

import java.util.List;

/**
 * reservation hold/release 的設計政策。
 *
 * 這個 policy 只整理語意與邊界，不會連到 repository、transaction 或任何正式 reservation runtime。
 */
public final class ReservationHoldReleaseDesignPolicy {

    /**
     * 建立 reservation hold/release 的設計契約。
     *
     * 這份輸出只描述未來 reservation runtime 的安全邊界，不代表已可直接執行。
     */
    public ReservationHoldReleaseDesign describe() {
        return new ReservationHoldReleaseDesign(
                ReservationLifecycleDecision.DESIGN_ONLY,
                List.of(
                        ReservationOperationType.HOLD,
                        ReservationOperationType.RELEASE,
                        ReservationOperationType.COMMIT,
                        ReservationOperationType.CANCEL
                ),
                List.of(
                        "hold 會降低 available_amount，並增加 locked_amount",
                        "release 會增加 available_amount，並降低 locked_amount",
                        "commit 代表 reserved amount 被消耗，必須進入 settlement / ledger posting gate",
                        "cancel / rollback 必須可追蹤、可審計、可重試"
                ),
                List.of(
                        "reservation 只能透過 application boundary 建立 / 釋放 / commit / cancel",
                        "hold / release 不等於 ledger transfer",
                        "ledger 是 source of truth",
                        "balance_projections 是 read model",
                        "order intake 可以要求 reservation，但不得直接寫 reservation DB",
                        "matching 不得偷寫 reservation 或 balance_projections"
                ),
                List.of(
                        "requestId 不是 idempotency guarantee",
                        "idempotency key 才能防 duplicate hold / release",
                        "reservation runtime 未來必須先做 idempotency decision，再做 hold / release",
                        "reservation commit 也必須保留 idempotency / audit linkage"
                ),
                List.of(
                        "不新增正式 reservation runtime",
                        "不更新 balance_projections",
                        "不接 ledger posting runtime",
                        "不接 order / matching / settlement runtime",
                        "所有 reservation runtime 都屬於 HUMAN_REVIEW_REQUIRED"
                )
        );
    }

    /**
     * 確認 hold / release 會影響 available 與 locked 語意。
     *
     * 這個方法只回傳設計意圖，不代表任何 runtime 已完成。
     */
    public boolean definesHoldReleaseAffectsAvailableAndLocked() {
        return true;
    }

    /**
     * 確認 reservation 只能透過 application boundary 操作。
     *
     * 這個方法只回傳設計意圖，不代表任何 runtime 已完成。
     */
    public boolean requiresApplicationBoundary() {
        return true;
    }

    /**
     * 確認 reservation 不可直接改 ledger。
     *
     * 這個方法只回傳設計意圖，不代表任何 runtime 已完成。
     */
    public boolean forbidsDirectLedgerMutation() {
        return true;
    }

    /**
     * 確認 reservation runtime 未來必須搭配 idempotency。
     *
     * 這個方法只回傳設計意圖，不代表任何 runtime 已完成。
     */
    public boolean requiresIdempotencyForRuntime() {
        return true;
    }

    /**
     * 確認 matching 不得直接寫 reservation。
     *
     * 這個方法只回傳設計意圖，不代表任何 runtime 已完成。
     */
    public boolean forbidsMatchingDirectWrites() {
        return true;
    }
}
