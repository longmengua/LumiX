package com.lumix.application.transaction;

/**
 * transaction boundary policy 用的 use case 分類。
 *
 * 這個分類只描述 boundary 風險，不代表 runtime 行為已經完成。
 */
public enum TransactionUseCase {
    READ_ONLY_QUERY,
    STATE_MUTATION,
    LEDGER_POSTING,
    RESERVATION_HOLD_RELEASE,
    WITHDRAWAL_REQUEST,
    ORDER_PLACEMENT,
    SETTLEMENT,
    OUTBOX_APPEND,
    IDEMPOTENCY_RECORD,
    AUDIT_APPEND
}
