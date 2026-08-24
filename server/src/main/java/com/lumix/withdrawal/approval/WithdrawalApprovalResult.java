package com.lumix.withdrawal.approval;

import java.util.List;
import java.util.Objects;

/** 純審核結果保留使用過的輸入證據，讓後續 audit 層可重放決策而不需要重查權限系統。 */
public record WithdrawalApprovalResult(WithdrawalApprovalDecision decision, List<WithdrawalApprovalEvidence> evidence) {
    public WithdrawalApprovalResult {
        decision = Objects.requireNonNull(decision, "decision");
        evidence = List.copyOf(Objects.requireNonNull(evidence, "evidence"));
    }

    /** 僅供下一個純 intent contract 判斷，不代表可以直接簽章。 */
    public boolean isApproved() {
        return decision == WithdrawalApprovalDecision.APPROVED;
    }
}
