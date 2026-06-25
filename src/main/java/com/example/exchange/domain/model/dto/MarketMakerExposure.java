/*
 * 檔案用途：做市商 inventory/exposure read model。
 */
package com.example.exchange.domain.model.dto;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;


@Data
@Builder
@Jacksonized
public class MarketMakerExposure {

    private final String marketMakerId;

    private final long uid;

    private final String symbol;

    private final BigDecimal quantity;

    private final BigDecimal markPrice;

    private final BigDecimal notional;
    public MarketMakerExposure(String marketMakerId, long uid, String symbol, BigDecimal quantity, BigDecimal markPrice, BigDecimal notional) {
        this.marketMakerId = marketMakerId;
        this.uid = uid;
        this.symbol = symbol;
        this.quantity = quantity;
        this.markPrice = markPrice;
        this.notional = notional;
    }

    public String marketMakerId() {
        return marketMakerId;
    }

    public long uid() {
        return uid;
    }

    public String symbol() {
        return symbol;
    }

    public BigDecimal quantity() {
        return quantity;
    }

    public BigDecimal markPrice() {
        return markPrice;
    }

    public BigDecimal notional() {
        return notional;
    }
}