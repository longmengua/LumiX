package com.lumix.wallet;

/**
 * 提現狀態。
 */
public enum WithdrawStatus {
    SUBMITTED,
    RISK_REVIEW,
    ADMIN_REVIEW,
    APPROVED,
    REJECTED,
    BROADCASTING,
    CHAIN_PENDING,
    SUCCESS,
    FAILED,
    CANCELED
}
