package com.lumix.withdrawal.domain;

/** 提款 runtime 唯一狀態集合；資料庫 constraint 與此 enum 必須同步演進。 */
public enum WithdrawalStatus {
    REQUESTED, FUNDS_HELD, RISK_PENDING, RISK_HOLD, MANUAL_REVIEW, APPROVED, REJECTED,
    FINAL_RISK_CHECK, SIGN_PENDING, SIGNING, SIGNED, BROADCASTING, BROADCASTED, CONFIRMING,
    COMPLETED, FAILED, CANCELLED
}
