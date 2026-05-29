/*
 * 檔案用途：領域 DTO，於服務、事件與介面層之間傳遞結構化資料。
 */
package com.example.exchange.domain.model.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record AdlQueueEntry(
        String liquidationId,
        long uid,
        String symbol,
        String liquidatedSide,
        BigDecimal amount,
        Instant ts,
        String status,
        String owner,
        Instant claimedAt
) {}
