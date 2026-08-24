package com.lumix.withdrawal.request;

import java.time.Instant;
import java.util.Objects;

/**
 * caller 提供時間的 immutable audit event；policy 不讀取系統時鐘，讓 replay 可重現。
 */
public record WithdrawalAuditEvent(
        WithdrawalRequestId requestId,
        WithdrawalAuditEventType type,
        Instant occurredAt
) {

    public WithdrawalAuditEvent {
        requestId = Objects.requireNonNull(requestId, "requestId must not be null");
        type = Objects.requireNonNull(type, "type must not be null");
        occurredAt = Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }
}
