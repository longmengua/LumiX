package com.lumix.withdrawal.request;

/**
 * P24 request 的初始狀態集合；後續 eligibility/cancel/approval phase 才能定義合法 transition。
 */
public enum WithdrawalRequestLifecycle {
    REQUESTED,
    ELIGIBILITY_PENDING,
    MANUAL_REVIEW_PENDING,
    CANCELLED,
    EXPIRED,
    APPROVAL_HANDOFF_READY
}
