package com.lumix.idempotency;

import java.time.Instant;
import java.util.Objects;

/**
 * 冪等紀錄。
 * Phase 9 只定義資料外形，實際儲存與併發控制留到後續 Phase。
 */
public record IdempotencyRecord(
        IdempotencyKey key,
        String requestHash,
        IdempotencyStatus status,
        Instant createdAt,
        Instant updatedAt,
        String responseReference
) {

    public IdempotencyRecord {
        // 基本追蹤欄位不能為空，否則冪等查詢與稽核會失去意義。
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }
}
