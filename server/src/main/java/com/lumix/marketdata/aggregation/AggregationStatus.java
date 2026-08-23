package com.lumix.marketdata.aggregation;

/**
 * 唯讀 trade aggregation 可供 consumer 判讀的完整性狀態。
 *
 * <p>只有 {@link #HEALTHY} 代表本 reducer 未觀察到缺號、晚到或完整性衝突；它不是交易成交真實性、
 * 行情服務 SLA 或 production-ready 的宣告。</p>
 */
public enum AggregationStatus {
    UNAVAILABLE,
    HEALTHY,
    STALE,
    GAP_DETECTED,
    RESYNC_REQUIRED,
    DEGRADED
}
