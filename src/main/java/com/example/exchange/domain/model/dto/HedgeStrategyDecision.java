/*
 * 檔案用途：做市商 inventory-aware hedge strategy 的決策結果。
 */
package com.example.exchange.domain.model.dto;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;


@Data
@Builder
@Jacksonized
public class HedgeStrategyDecision {

    private final String marketMakerId;

    private final String symbol;

    private final boolean hedgeRequired;

    private final String reason;

    private final MarketMakerExposure exposure;

    private final HedgeOrderRequest orderRequest;
    public HedgeStrategyDecision(String marketMakerId, String symbol, boolean hedgeRequired, String reason, MarketMakerExposure exposure, HedgeOrderRequest orderRequest) {
        this.marketMakerId = marketMakerId;
        this.symbol = symbol;
        this.hedgeRequired = hedgeRequired;
        this.reason = reason;
        this.exposure = exposure;
        this.orderRequest = orderRequest;
    }

    public String marketMakerId() {
        return marketMakerId;
    }

    public String symbol() {
        return symbol;
    }

    public boolean hedgeRequired() {
        return hedgeRequired;
    }

    public String reason() {
        return reason;
    }

    public MarketMakerExposure exposure() {
        return exposure;
    }

    public HedgeOrderRequest orderRequest() {
        return orderRequest;
    }
}