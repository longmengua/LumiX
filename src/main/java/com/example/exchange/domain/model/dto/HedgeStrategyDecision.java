/*
 * 檔案用途：做市商 inventory-aware hedge strategy 的決策結果。
 */
package com.example.exchange.domain.model.dto;

public record HedgeStrategyDecision(
        String marketMakerId,
        String symbol,
        boolean hedgeRequired,
        String reason,
        MarketMakerExposure exposure,
        HedgeOrderRequest orderRequest
) {
}
