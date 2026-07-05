package com.lumix.idempotency;

/**
 * 冪等紀錄狀態。
 * 先保留流程狀態機骨架，不聲稱已完成 distributed lock 或 exactly-once。
 */
public enum IdempotencyStatus {
    NEW,
    PROCESSING,
    SUCCEEDED,
    FAILED,
    EXPIRED
}
