/*
 * 檔案用途：送往外部 hedge venue 的對沖訂單 request。
 */
package com.example.exchange.domain.model.dto;

import com.example.exchange.domain.model.enums.OrderSide;

import java.math.BigDecimal;

public record HedgeOrderRequest(
        String marketMakerId,
        long uid,
        String symbol,
        OrderSide side,
        BigDecimal quantity,
        BigDecimal referencePrice,
        BigDecimal limitPrice,
        String refId
) {
}
