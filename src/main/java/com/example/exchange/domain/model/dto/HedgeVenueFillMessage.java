/*
 * 檔案用途：外部 hedge venue 成交回報的標準化 message。
 */
package com.example.exchange.domain.model.dto;

import com.example.exchange.domain.model.enums.OrderSide;

import java.math.BigDecimal;
import java.time.Instant;

public record HedgeVenueFillMessage(
        String marketMakerId,
        String symbol,
        String venueOrderId,
        String venueFillId,
        OrderSide side,
        BigDecimal quantity,
        BigDecimal price,
        BigDecimal fee,
        String feeAsset,
        String refId,
        Instant filledAt
) {
}
