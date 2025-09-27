package com.example.exchange.interfaces.web.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import com.example.exchange.domain.model.OrderSide;
import com.example.exchange.domain.model.OrderType;


/**
 * 下單 API Request DTO
 * - 使用 Bean Validation 做基本參數檢查
 */
public record PlaceOrderRequest(
        @NotNull Long uid,
        @NotBlank String symbol,      // 例如 "BTCUSDT"
        @NotNull OrderSide side,      // BUY / SELL
        @NotNull OrderType type,      // MARKET / LIMIT
        BigDecimal price,             // 市價單可為 null
        @DecimalMin("0.0001") BigDecimal qty,
        @Min(1) @Max(125) Integer leverage,
        @NotBlank String marginMode   // "CROSS" / "ISOLATED"
) {}
