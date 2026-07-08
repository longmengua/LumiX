package com.lumix.ledger.application.posting;

import java.util.Objects;
import java.util.Optional;

/**
 * ledger posting command boundary 的結果。
 *
 * 這個結果只會是 accepted 或 rejected，不會暗示已完成寫入或已完成交易。
 */
public record LedgerPostingCommandResult(
        LedgerPostingDecision decision,
        LedgerPostingPlan acceptedPlan,
        LedgerPostingRejection rejectionReason
) {

    public LedgerPostingCommandResult {
        // 結果型別必須明確，避免上層把 plan 誤當成正式資金異動完成。
        Objects.requireNonNull(decision, "decision must not be null");
        if (decision == LedgerPostingDecision.ACCEPTED) {
            Objects.requireNonNull(acceptedPlan, "acceptedPlan must not be null when accepted");
            if (rejectionReason != null) {
                throw new IllegalArgumentException("rejectionReason must be null when accepted");
            }
        } else {
            Objects.requireNonNull(rejectionReason, "rejectionReason must not be null when rejected");
            if (acceptedPlan != null) {
                throw new IllegalArgumentException("acceptedPlan must be null when rejected");
            }
        }
    }

    /**
     * 建立 accepted 結果。
     *
     * 只代表 command 通過 gate 並完成 plan 建構，不代表任何資料已寫入。
     */
    public static LedgerPostingCommandResult accepted(LedgerPostingPlan plan) {
        return new LedgerPostingCommandResult(LedgerPostingDecision.ACCEPTED, plan, null);
    }

    /**
     * 建立 rejected 結果。
     *
     * 只回傳安全原因，避免外洩底層 client 或資料庫細節。
     */
    public static LedgerPostingCommandResult rejected(LedgerPostingRejection rejection) {
        return new LedgerPostingCommandResult(LedgerPostingDecision.REJECTED, null, rejection);
    }

    /**
     * 讓呼叫端以不拋例外的方式取得 accepted plan。
     */
    public Optional<LedgerPostingPlan> plan() {
        return Optional.ofNullable(acceptedPlan);
    }

    /**
     * 讓呼叫端以不拋例外的方式取得 rejected reason。
     */
    public Optional<LedgerPostingRejection> rejection() {
        return Optional.ofNullable(rejectionReason);
    }
}
