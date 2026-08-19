package com.lumix.marketdata.policy;

/**
 * 行情 stream 可供下游判讀的健康狀態。
 *
 * <p>只有 {@link #HEALTHY} 可被視為即時正常資料。其餘狀態必須隨 decision 一起保留，
 * 避免 projection 或未來 transport 把 stale、缺號或完整性衝突的資料誤標為正常行情。</p>
 */
public enum FeedHealth {
    HEALTHY,
    STALE,
    GAP_DETECTED,
    RESYNC_REQUIRED,
    DEGRADED,
    STOPPED
}
