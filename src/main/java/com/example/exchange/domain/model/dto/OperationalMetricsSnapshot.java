/*
 * 檔案用途：領域 DTO，於服務、事件與介面層之間傳遞結構化資料。
 */
package com.example.exchange.domain.model.dto;

/**
 * 輕量營運 metrics 快照。
 *
 * <p>這是 in-process counters 的讀模型，服務重啟會歸零。production 應接 metrics backend。</p>
 */
public record OperationalMetricsSnapshot(
        /** 下單請求完成並記錄結果的總數。 */
        long orderRequests,
        /** 最終狀態為 NEW 的訂單數。 */
        long orderNew,
        /** 最終狀態為 PARTIALLY_FILLED 的訂單數。 */
        long orderPartiallyFilled,
        /** 最終狀態為 FILLED 的訂單數。 */
        long orderFilled,
        /** 風控或撮合拒絕的訂單數。 */
        long orderRejected,
        /** 主動撤單、批量撤單、斷線撤單的合計。 */
        long orderCanceled,
        /** IOC/FOK/MARKET 流動性不足等失效訂單數。 */
        long orderExpired,
        /** 已觀測到的 trade event 數。 */
        long tradeEvents,
        /** 有 latency 樣本的下單流程數。 */
        long orderLatencyCount,
        /** 平均下單延遲，單位毫秒。 */
        long orderLatencyAvgMs,
        /** 最大下單延遲，單位毫秒。 */
        long orderLatencyMaxMs,
        /** 已納入 matching 指標的下單嘗試數。 */
        long matchingRequests,
        /** 最終狀態為 REJECTED 的 matching 嘗試數。 */
        long matchingRejected,
        /** 最終狀態有成交量的 matching 嘗試數。 */
        long matchingFilled,
        /** matchingRejected / matchingRequests，沒有樣本時為 0。 */
        double matchingRejectionRate,
        /** matchingFilled / matchingRequests，沒有樣本時為 0。 */
        double matchingFillRate,
        /** 有 matching latency 樣本的流程數。 */
        long matchingLatencyCount,
        /** 平均 matching latency，單位毫秒。 */
        long matchingLatencyAvgMs,
        /** 最大 matching latency，單位毫秒。 */
        long matchingLatencyMaxMs,
        /** 有 DB operation latency 樣本的流程數。 */
        long databaseLatencyCount,
        /** 平均 DB operation latency，單位毫秒。 */
        long databaseLatencyAvgMs,
        /** 最大 DB operation latency，單位毫秒。 */
        long databaseLatencyMaxMs,
        /** 有 Redis operation latency 樣本的流程數。 */
        long redisLatencyCount,
        /** 平均 Redis operation latency，單位毫秒。 */
        long redisLatencyAvgMs,
        /** 最大 Redis operation latency，單位毫秒。 */
        long redisLatencyMaxMs
) {}
