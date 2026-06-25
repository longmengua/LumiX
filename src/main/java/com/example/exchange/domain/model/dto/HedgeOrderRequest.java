/*
 * 檔案用途：送往外部 hedge venue 的對沖訂單 request。
 */
package com.example.exchange.domain.model.dto;

import com.example.exchange.domain.model.enums.OrderSide;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;


@Data
@Builder
@Jacksonized
public class HedgeOrderRequest {

    private final String marketMakerId;

    private final long uid;

    private final String symbol;

    private final OrderSide side;

    private final BigDecimal quantity;

    private final BigDecimal referencePrice;

    private final BigDecimal limitPrice;

    private final String refId;
    public HedgeOrderRequest(String marketMakerId, long uid, String symbol, OrderSide side, BigDecimal quantity, BigDecimal referencePrice, BigDecimal limitPrice, String refId) {
        this.marketMakerId = marketMakerId;
        this.uid = uid;
        this.symbol = symbol;
        this.side = side;
        this.quantity = quantity;
        this.referencePrice = referencePrice;
        this.limitPrice = limitPrice;
        this.refId = refId;
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

    public OrderSide side() {
        return side;
    }

    public BigDecimal quantity() {
        return quantity;
    }

    public BigDecimal referencePrice() {
        return referencePrice;
    }

    public BigDecimal limitPrice() {
        return limitPrice;
    }

    public String refId() {
        return refId;
    }
}