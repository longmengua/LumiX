/*
 * 檔案用途：領域 DTO，於服務、事件與介面層之間傳遞結構化資料。
 */
package com.example.exchange.domain.model.dto;

import com.example.exchange.domain.model.enums.OrderSide;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TradeTapeItem(
        String symbol,
        String matchId,
        UUID orderId,
        OrderSide side,
        BigDecimal price,
        BigDecimal qty,
        boolean maker,
        Instant ts
) {}
