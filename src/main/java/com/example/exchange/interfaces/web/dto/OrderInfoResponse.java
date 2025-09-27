package com.example.exchange.interfaces.web.dto;

import com.example.exchange.domain.model.OrderSide;
import com.example.exchange.domain.model.OrderType;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * API 專用的訂單回應 DTO
 */
public record OrderInfoResponse(
        String orderId,
        Long uid,
        String symbol,
        OrderSide side,
        OrderType type,
        BigDecimal price,
        BigDecimal qty,
        String status,
        Instant ctime
) {}
