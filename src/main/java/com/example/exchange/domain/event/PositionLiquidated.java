package com.example.exchange.domain.event;

import com.example.exchange.domain.model.entity.Symbol;

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
        BigDecimal bankruptedPrice,
        Instant ts
) {}
