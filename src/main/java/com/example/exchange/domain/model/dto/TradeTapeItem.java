/*
 * 檔案用途：領域 DTO，於服務、事件與介面層之間傳遞結構化資料。
 */
package com.example.exchange.domain.model.dto;

import com.example.exchange.domain.model.enums.OrderSide;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;


@Data
@Builder
@Jacksonized
public class TradeTapeItem {

    private final String symbol;

    private final String matchId;

    private final UUID orderId;

    private final OrderSide side;

    private final BigDecimal price;

    private final BigDecimal qty;

    private final boolean maker;

    private final Instant ts;
    public TradeTapeItem(String symbol, String matchId, UUID orderId, OrderSide side, BigDecimal price, BigDecimal qty, boolean maker, Instant ts) {
        this.symbol = symbol;
        this.matchId = matchId;
        this.orderId = orderId;
        this.side = side;
        this.price = price;
        this.qty = qty;
        this.maker = maker;
        this.ts = ts;
    }

    public String symbol() {
        return symbol;
    }

    public String matchId() {
        return matchId;
    }

    public UUID orderId() {
        return orderId;
    }

    public OrderSide side() {
        return side;
    }

    public BigDecimal price() {
        return price;
    }

    public BigDecimal qty() {
        return qty;
    }

    public boolean maker() {
        return maker;
    }

    public Instant ts() {
        return ts;
    }
}