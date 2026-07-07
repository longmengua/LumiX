package com.lumix.application.transaction;

/**
 * transaction boundary 類型。
 *
 * 這裡只區分 read-only 與 write，避免把高風險流程誤解成普通查詢。
 */
public enum TransactionBoundaryKind {
    READ_ONLY,
    WRITE
}
