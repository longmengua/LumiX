package com.lumix.withdrawal.request;

/** 本 task 僅允許建立不可變 request-created audit event。 */
public enum WithdrawalAuditEventType {
    REQUEST_CREATED,
    CANCELLATION_RECORDED,
    EXPIRATION_RECORDED,
    MANUAL_REVIEW_QUEUED,
    APPROVAL_HANDOFF_RECORDED
}
