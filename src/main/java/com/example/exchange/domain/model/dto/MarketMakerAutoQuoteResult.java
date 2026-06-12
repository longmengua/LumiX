/*
 * 檔案用途：做市商自動報價 DTO，描述單一 profile/symbol 的策略執行結果。
 */
package com.example.exchange.domain.model.dto;

public record MarketMakerAutoQuoteResult(
        String marketMakerId,
        String symbol,
        boolean placed,
        String reason,
        String refId
) {
}
