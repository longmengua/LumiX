package com.lumix.withdrawal.approval;

/** 審核 policy 的 fail-closed outcome；APPROVED 也不是任何 signer 或 broadcast 命令。 */
public enum WithdrawalApprovalDecision {
    APPROVED,
    SIGNER_INPUT_INVALID_REJECTED,
    APPROVAL_EXPIRED_REJECTED,
    AMOUNT_LIMIT_REJECTED,
    REQUEST_OWNER_REJECTED,
    DUPLICATE_REVIEWER_REJECTED,
    MISSING_REQUIRED_ROLE_REJECTED
}
