/*
 * 檔案用途：領域事件，描述交易、快照、資金費或強平等已發生的業務事實。
 */
package com.example.exchange.domain.event;

import com.example.exchange.domain.model.dto.Symbol;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 強平事件
 *
 * - 當倉位觸發維持保證金不足時，產生強平事件
 * - bankruptedPrice: 破產價格或最終成交均價（視你的風控/清算邏輯而定）
 */
public record PositionLiquidated(
        long uid,
        Symbol symbol,
        BigDecimal liquidationPrice,
        BigDecimal markPrice,
        BigDecimal closedQty,
        BigDecimal realizedPnl,
        BigDecimal insuranceFundCovered,
        BigDecimal adlCovered,
        Instant ts
) {
    public PositionLiquidated(long uid, Symbol symbol, BigDecimal bankruptedPrice, Instant ts) {
        this(uid, symbol, bankruptedPrice, bankruptedPrice, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, ts);
    }
}
