package com.example.java21_OLAP.interfaces.web.dto;

import com.example.java21_OLAP.domain.model.OrderSide;
import com.example.java21_OLAP.domain.model.OrderType;

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
        BigDecimal remainQty,
        String status,
        Instant ctime
) {}
