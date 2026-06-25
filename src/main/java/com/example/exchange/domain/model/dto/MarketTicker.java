/*
 * 檔案用途：領域 DTO，於服務、事件與介面層之間傳遞結構化資料。
 */
package com.example.exchange.domain.model.dto;

import java.math.BigDecimal;
import java.time.Instant;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;


@Data
@Builder
@Jacksonized
public class MarketTicker {

    private final String symbol;

    private final BigDecimal lastPrice;

    private final BigDecimal bestBid;

    private final BigDecimal bestAsk;

    private final BigDecimal volume24h;

    private final BigDecimal high24h;

    private final BigDecimal low24h;

    private final Instant updatedAt;
    public MarketTicker(String symbol, BigDecimal lastPrice, BigDecimal bestBid, BigDecimal bestAsk, BigDecimal volume24h, BigDecimal high24h, BigDecimal low24h, Instant updatedAt) {
        this.symbol = symbol;
        this.lastPrice = lastPrice;
        this.bestBid = bestBid;
        this.bestAsk = bestAsk;
        this.volume24h = volume24h;
        this.high24h = high24h;
        this.low24h = low24h;
        this.updatedAt = updatedAt;
    }

    public String symbol() {
        return symbol;
    }

    public BigDecimal lastPrice() {
        return lastPrice;
    }

    public BigDecimal bestBid() {
        return bestBid;
    }

    public BigDecimal bestAsk() {
        return bestAsk;
    }

    public BigDecimal volume24h() {
        return volume24h;
    }

    public BigDecimal high24h() {
        return high24h;
    }

    public BigDecimal low24h() {
        return low24h;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}