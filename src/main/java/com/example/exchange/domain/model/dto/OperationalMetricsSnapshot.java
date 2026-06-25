/*
 * 檔案用途：領域 DTO，於服務、事件與介面層之間傳遞結構化資料。
 */
package com.example.exchange.domain.model.dto;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;


/**
 * 輕量營運 metrics 快照。
 *
 * <p>這是 in-process counters 的讀模型，服務重啟會歸零。production 應接 metrics backend。</p>
 */
@Data
@Builder
@Jacksonized
public class OperationalMetricsSnapshot {

    private final /** 下單請求完成並記錄結果的總數。 */
        long orderRequests;

    private final /** 最終狀態為 NEW 的訂單數。 */
        long orderNew;

    private final /** 最終狀態為 PARTIALLY_FILLED 的訂單數。 */
        long orderPartiallyFilled;

    private final /** 最終狀態為 FILLED 的訂單數。 */
        long orderFilled;

    private final /** 風控或撮合拒絕的訂單數。 */
        long orderRejected;

    private final /** 主動撤單、批量撤單、斷線撤單的合計。 */
        long orderCanceled;

    private final /** IOC/FOK/MARKET 流動性不足等失效訂單數。 */
        long orderExpired;

    private final /** 已觀測到的 trade event 數。 */
        long tradeEvents;

    private final /** 有 latency 樣本的下單流程數。 */
        long orderLatencyCount;

    private final /** 平均下單延遲，單位毫秒。 */
        long orderLatencyAvgMs;

    private final /** 最大下單延遲，單位毫秒。 */
        long orderLatencyMaxMs;

    private final /** 已納入 matching 指標的下單嘗試數。 */
        long matchingRequests;

    private final /** 最終狀態為 REJECTED 的 matching 嘗試數。 */
        long matchingRejected;

    private final /** 最終狀態有成交量的 matching 嘗試數。 */
        long matchingFilled;

    private final /** matchingRejected / matchingRequests，沒有樣本時為 0。 */
        double matchingRejectionRate;

    private final /** matchingFilled / matchingRequests，沒有樣本時為 0。 */
        double matchingFillRate;

    private final /** 有 matching latency 樣本的流程數。 */
        long matchingLatencyCount;

    private final /** 平均 matching latency，單位毫秒。 */
        long matchingLatencyAvgMs;

    private final /** 最大 matching latency，單位毫秒。 */
        long matchingLatencyMaxMs;

    private final /** 有 DB operation latency 樣本的流程數。 */
        long databaseLatencyCount;

    private final /** 平均 DB operation latency，單位毫秒。 */
        long databaseLatencyAvgMs;

    private final /** 最大 DB operation latency，單位毫秒。 */
        long databaseLatencyMaxMs;

    private final /** 有 Redis operation latency 樣本的流程數。 */
        long redisLatencyCount;

    private final /** 平均 Redis operation latency，單位毫秒。 */
        long redisLatencyAvgMs;

    private final /** 最大 Redis operation latency，單位毫秒。 */
        long redisLatencyMaxMs;

    private final /** 已採樣 Kafka consumer partitions 數。 */
        long kafkaLagPartitions;

    private final /** Kafka consumer lag 總和，單位為 messages。 */
        long kafkaLagTotal;

    private final /** 最大單 partition Kafka consumer lag，單位為 messages。 */
        long kafkaLagMax;
    public OperationalMetricsSnapshot(/** 下單請求完成並記錄結果的總數。 */
        long orderRequests, /** 最終狀態為 NEW 的訂單數。 */
        long orderNew, /** 最終狀態為 PARTIALLY_FILLED 的訂單數。 */
        long orderPartiallyFilled, /** 最終狀態為 FILLED 的訂單數。 */
        long orderFilled, /** 風控或撮合拒絕的訂單數。 */
        long orderRejected, /** 主動撤單、批量撤單、斷線撤單的合計。 */
        long orderCanceled, /** IOC/FOK/MARKET 流動性不足等失效訂單數。 */
        long orderExpired, /** 已觀測到的 trade event 數。 */
        long tradeEvents, /** 有 latency 樣本的下單流程數。 */
        long orderLatencyCount, /** 平均下單延遲，單位毫秒。 */
        long orderLatencyAvgMs, /** 最大下單延遲，單位毫秒。 */
        long orderLatencyMaxMs, /** 已納入 matching 指標的下單嘗試數。 */
        long matchingRequests, /** 最終狀態為 REJECTED 的 matching 嘗試數。 */
        long matchingRejected, /** 最終狀態有成交量的 matching 嘗試數。 */
        long matchingFilled, /** matchingRejected / matchingRequests，沒有樣本時為 0。 */
        double matchingRejectionRate, /** matchingFilled / matchingRequests，沒有樣本時為 0。 */
        double matchingFillRate, /** 有 matching latency 樣本的流程數。 */
        long matchingLatencyCount, /** 平均 matching latency，單位毫秒。 */
        long matchingLatencyAvgMs, /** 最大 matching latency，單位毫秒。 */
        long matchingLatencyMaxMs, /** 有 DB operation latency 樣本的流程數。 */
        long databaseLatencyCount, /** 平均 DB operation latency，單位毫秒。 */
        long databaseLatencyAvgMs, /** 最大 DB operation latency，單位毫秒。 */
        long databaseLatencyMaxMs, /** 有 Redis operation latency 樣本的流程數。 */
        long redisLatencyCount, /** 平均 Redis operation latency，單位毫秒。 */
        long redisLatencyAvgMs, /** 最大 Redis operation latency，單位毫秒。 */
        long redisLatencyMaxMs, /** 已採樣 Kafka consumer partitions 數。 */
        long kafkaLagPartitions, /** Kafka consumer lag 總和，單位為 messages。 */
        long kafkaLagTotal, /** 最大單 partition Kafka consumer lag，單位為 messages。 */
        long kafkaLagMax) {
        this.orderRequests = orderRequests;
        this.orderNew = orderNew;
        this.orderPartiallyFilled = orderPartiallyFilled;
        this.orderFilled = orderFilled;
        this.orderRejected = orderRejected;
        this.orderCanceled = orderCanceled;
        this.orderExpired = orderExpired;
        this.tradeEvents = tradeEvents;
        this.orderLatencyCount = orderLatencyCount;
        this.orderLatencyAvgMs = orderLatencyAvgMs;
        this.orderLatencyMaxMs = orderLatencyMaxMs;
        this.matchingRequests = matchingRequests;
        this.matchingRejected = matchingRejected;
        this.matchingFilled = matchingFilled;
        this.matchingRejectionRate = matchingRejectionRate;
        this.matchingFillRate = matchingFillRate;
        this.matchingLatencyCount = matchingLatencyCount;
        this.matchingLatencyAvgMs = matchingLatencyAvgMs;
        this.matchingLatencyMaxMs = matchingLatencyMaxMs;
        this.databaseLatencyCount = databaseLatencyCount;
        this.databaseLatencyAvgMs = databaseLatencyAvgMs;
        this.databaseLatencyMaxMs = databaseLatencyMaxMs;
        this.redisLatencyCount = redisLatencyCount;
        this.redisLatencyAvgMs = redisLatencyAvgMs;
        this.redisLatencyMaxMs = redisLatencyMaxMs;
        this.kafkaLagPartitions = kafkaLagPartitions;
        this.kafkaLagTotal = kafkaLagTotal;
        this.kafkaLagMax = kafkaLagMax;
    }

    public /** 下單請求完成並記錄結果的總數。 */
        long orderRequests() {
        return orderRequests;
    }

    public /** 最終狀態為 NEW 的訂單數。 */
        long orderNew() {
        return orderNew;
    }

    public /** 最終狀態為 PARTIALLY_FILLED 的訂單數。 */
        long orderPartiallyFilled() {
        return orderPartiallyFilled;
    }

    public /** 最終狀態為 FILLED 的訂單數。 */
        long orderFilled() {
        return orderFilled;
    }

    public /** 風控或撮合拒絕的訂單數。 */
        long orderRejected() {
        return orderRejected;
    }

    public /** 主動撤單、批量撤單、斷線撤單的合計。 */
        long orderCanceled() {
        return orderCanceled;
    }

    public /** IOC/FOK/MARKET 流動性不足等失效訂單數。 */
        long orderExpired() {
        return orderExpired;
    }

    public /** 已觀測到的 trade event 數。 */
        long tradeEvents() {
        return tradeEvents;
    }

    public /** 有 latency 樣本的下單流程數。 */
        long orderLatencyCount() {
        return orderLatencyCount;
    }

    public /** 平均下單延遲，單位毫秒。 */
        long orderLatencyAvgMs() {
        return orderLatencyAvgMs;
    }

    public /** 最大下單延遲，單位毫秒。 */
        long orderLatencyMaxMs() {
        return orderLatencyMaxMs;
    }

    public /** 已納入 matching 指標的下單嘗試數。 */
        long matchingRequests() {
        return matchingRequests;
    }

    public /** 最終狀態為 REJECTED 的 matching 嘗試數。 */
        long matchingRejected() {
        return matchingRejected;
    }

    public /** 最終狀態有成交量的 matching 嘗試數。 */
        long matchingFilled() {
        return matchingFilled;
    }

    public /** matchingRejected / matchingRequests，沒有樣本時為 0。 */
        double matchingRejectionRate() {
        return matchingRejectionRate;
    }

    public /** matchingFilled / matchingRequests，沒有樣本時為 0。 */
        double matchingFillRate() {
        return matchingFillRate;
    }

    public /** 有 matching latency 樣本的流程數。 */
        long matchingLatencyCount() {
        return matchingLatencyCount;
    }

    public /** 平均 matching latency，單位毫秒。 */
        long matchingLatencyAvgMs() {
        return matchingLatencyAvgMs;
    }

    public /** 最大 matching latency，單位毫秒。 */
        long matchingLatencyMaxMs() {
        return matchingLatencyMaxMs;
    }

    public /** 有 DB operation latency 樣本的流程數。 */
        long databaseLatencyCount() {
        return databaseLatencyCount;
    }

    public /** 平均 DB operation latency，單位毫秒。 */
        long databaseLatencyAvgMs() {
        return databaseLatencyAvgMs;
    }

    public /** 最大 DB operation latency，單位毫秒。 */
        long databaseLatencyMaxMs() {
        return databaseLatencyMaxMs;
    }

    public /** 有 Redis operation latency 樣本的流程數。 */
        long redisLatencyCount() {
        return redisLatencyCount;
    }

    public /** 平均 Redis operation latency，單位毫秒。 */
        long redisLatencyAvgMs() {
        return redisLatencyAvgMs;
    }

    public /** 最大 Redis operation latency，單位毫秒。 */
        long redisLatencyMaxMs() {
        return redisLatencyMaxMs;
    }

    public /** 已採樣 Kafka consumer partitions 數。 */
        long kafkaLagPartitions() {
        return kafkaLagPartitions;
    }

    public /** Kafka consumer lag 總和，單位為 messages。 */
        long kafkaLagTotal() {
        return kafkaLagTotal;
    }

    public /** 最大單 partition Kafka consumer lag，單位為 messages。 */
        long kafkaLagMax() {
        return kafkaLagMax;
    }
}