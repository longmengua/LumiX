package com.lumix.marketdata.replay;

/** 不連線 resync request 的原因；request 只描述需要什麼，不會自行拉取 snapshot。 */
public enum ResyncReason {
    ADMISSION_GAP_OR_RESYNC,
    PROJECTION_REJECTED_OR_DEGRADED
}
