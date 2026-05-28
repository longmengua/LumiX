/*
 * 檔案用途：成交流水 read model，保存可對帳的 trade notional fact。
 */
package com.example.exchange.domain.model.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * 單筆成交流水事實。
 *
 * <p>Turnover 是交易量統計與活動門檻的依據，必須從 trade tape 產生，
 * 且保留 match/order 維度，讓後續可反查成交事件與 ledger ref。</p>
 */
public record TurnoverRecord(
        UUID id,
        long uid,
        String accountId,
        String symbol,
        String strategyId,
        String marketMakerId,
        UUID orderId,
        String matchId,
        long tradeSeq,
        BigDecimal quantity,
        BigDecimal price,
        BigDecimal notional,
        Instant tradedAt,
        Instant createdAt
) {
    public TurnoverRecord {
        id = id == null ? UUID.randomUUID() : id;
        accountId = blankToNull(accountId);
        strategyId = blankToNull(strategyId);
        marketMakerId = blankToNull(marketMakerId);
        quantity = quantity == null ? BigDecimal.ZERO : quantity.abs();
        price = price == null ? BigDecimal.ZERO : price;
        notional = notional == null ? price.multiply(quantity) : notional;
        tradedAt = tradedAt == null ? Instant.now() : tradedAt;
        createdAt = createdAt == null ? Instant.now() : createdAt;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
