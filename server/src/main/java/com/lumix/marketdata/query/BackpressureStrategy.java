package com.lumix.marketdata.query;

/** 慢 consumer 的明確處置；兩種結果都要求完整 resnapshot，禁止靜默略過 delta。 */
public enum BackpressureStrategy {
    DROP_AND_RESNAPSHOT,
    DISCONNECT_AND_RESNAPSHOT
}
