package com.lumix.marketdata.replay;

/** 可供 audit trace 與後續 recovery policy 使用的固定 replay 失敗原因。 */
public enum ReplayFailureReason {
    AMBIGUOUS_CANONICAL_ORDER,
    STREAM_LIMIT_EXCEEDED
}
