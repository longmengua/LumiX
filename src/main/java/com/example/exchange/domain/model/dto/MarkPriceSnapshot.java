/*
 * 檔案用途：領域 DTO，表示 mark/index price oracle 最新快照。
 */
package com.example.exchange.domain.model.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record MarkPriceSnapshot(
        String symbol,
        BigDecimal markPrice,
        BigDecimal indexPrice,
        String source,
        Instant updatedAt,
        boolean stale
) {
}
