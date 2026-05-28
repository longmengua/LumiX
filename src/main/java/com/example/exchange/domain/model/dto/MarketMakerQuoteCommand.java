/*
 * 檔案用途：做市商 quote command DTO，描述想要掛出的雙邊報價。
 */
package com.example.exchange.domain.model.dto;

import java.math.BigDecimal;

public record MarketMakerQuoteCommand(
        String marketMakerId,
        long uid,
        String symbol,
        BigDecimal bidPrice,
        BigDecimal bidQuantity,
        BigDecimal askPrice,
        BigDecimal askQuantity,
        String refId
) {
}
