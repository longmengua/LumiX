/*
 * 檔案用途：做市商 inventory/exposure read model。
 */
package com.example.exchange.domain.model.dto;

import java.math.BigDecimal;

public record MarketMakerExposure(
        String marketMakerId,
        long uid,
        String symbol,
        BigDecimal quantity,
        BigDecimal markPrice,
        BigDecimal notional
) {
}
