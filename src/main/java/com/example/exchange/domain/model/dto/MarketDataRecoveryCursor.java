/*
 * 檔案用途：領域 DTO，描述 market-data client 斷線重連時可提交的恢復游標。
 */
package com.example.exchange.domain.model.dto;

import java.time.Instant;

public record MarketDataRecoveryCursor(
        String symbol,
        long depthVersion,
        Instant tradeTs,
        String tradeMatchId,
        Instant generatedAt
) {
}
