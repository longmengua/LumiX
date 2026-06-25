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
public class MarketKline {

    private final String symbol;

    private final String interval;

    private final Instant openTime;

    private final BigDecimal open;

    private final BigDecimal high;

    private final BigDecimal low;

    private final BigDecimal close;

    private final BigDecimal volume;
    public MarketKline(String symbol, String interval, Instant openTime, BigDecimal open, BigDecimal high, BigDecimal low, BigDecimal close, BigDecimal volume) {
        this.symbol = symbol;
        this.interval = interval;
        this.openTime = openTime;
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.volume = volume;
    }

    public String symbol() {
        return symbol;
    }

    public String interval() {
        return interval;
    }

    public Instant openTime() {
        return openTime;
    }

    public BigDecimal open() {
        return open;
    }

    public BigDecimal high() {
        return high;
    }

    public BigDecimal low() {
        return low;
    }

    public BigDecimal close() {
        return close;
    }

    public BigDecimal volume() {
        return volume;
    }
}