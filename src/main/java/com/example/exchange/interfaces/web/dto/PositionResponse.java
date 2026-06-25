/*
 * 檔案用途：Web DTO，讓客戶端用穩定欄位讀取目前持倉，不直接暴露 domain model。
 */
package com.example.exchange.interfaces.web.dto;

import com.example.exchange.domain.model.dto.Position;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Current user position shown in the client trading activity panel.
 */
public record PositionResponse(
        Long uid,
        String symbol,
        String side,
        BigDecimal qty,
        BigDecimal entryPrice,
        BigDecimal margin,
        BigDecimal realizedPnl,
        Instant updatedAt
) {
    public static PositionResponse from(Position position) {
        BigDecimal qty = position.getQty() == null ? BigDecimal.ZERO : position.getQty();
        return new PositionResponse(
                position.getUid(),
                position.getSymbol() == null ? null : position.getSymbol().code(),
                qty.signum() < 0 ? "SHORT" : "LONG",
                qty.abs(),
                position.getEntryPrice(),
                position.getMargin(),
                position.getRealizedPnl(),
                position.getUpdatedAt()
        );
    }
}
