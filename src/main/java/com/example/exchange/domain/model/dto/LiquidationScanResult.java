/*
 * 檔案用途：領域 DTO，承載 liquidation scan 執行結果。
 */
package com.example.exchange.domain.model.dto;

import java.time.Instant;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;


/**
 * Liquidation scan result。
 *
 * @param scannedPositions 掃描的 open position 數量
 * @param liquidationCount 觸發 liquidation 的數量
 * @param reviewedCount    未平倉但進入覆核或不需強平的數量
 * @param results          每個 position 的 liquidation 判斷結果
 * @param scannedAt        掃描時間
 */
@Data
@Builder
@Jacksonized
public class LiquidationScanResult {

    private final int scannedPositions;

    private final int liquidationCount;

    private final int reviewedCount;

    private final List<LiquidationResult> results;

    private final Instant scannedAt;
    public LiquidationScanResult(int scannedPositions, int liquidationCount, int reviewedCount, List<LiquidationResult> results, Instant scannedAt) {
        this.scannedPositions = scannedPositions;
        this.liquidationCount = liquidationCount;
        this.reviewedCount = reviewedCount;
        this.results = results;
        this.scannedAt = scannedAt;
    }

    public int scannedPositions() {
        return scannedPositions;
    }

    public int liquidationCount() {
        return liquidationCount;
    }

    public int reviewedCount() {
        return reviewedCount;
    }

    public List<LiquidationResult> results() {
        return results;
    }

    public Instant scannedAt() {
        return scannedAt;
    }
}