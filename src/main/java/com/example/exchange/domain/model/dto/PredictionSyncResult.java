/*
 * 檔案用途：領域 DTO，於服務、事件與介面層之間傳遞結構化資料。
 */
package com.example.exchange.domain.model.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * Prediction market key sync result.
 */
@Getter
@Builder
public class PredictionSyncResult {

    private final String status;

    private final int totalCount;

    private final int successCount;

    private final int failedCount;

    private final int skippedCount;

    private final String message;
}
