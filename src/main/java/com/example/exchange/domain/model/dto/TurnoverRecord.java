/*
 * 檔案用途：成交流水 read model，保存可對帳的 trade notional fact。
 */
package com.example.exchange.domain.model.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;


/**
 * 單筆成交流水事實。
 *
 * <p>Turnover 是交易量統計與活動門檻的依據，必須從 trade tape 產生，
 * 且保留 match/order 維度，讓後續可反查成交事件與 ledger ref。</p>
 */
@Data
@Builder
@Jacksonized
public class TurnoverRecord {

    private final UUID id;

    private final long uid;

    private final String accountId;

    private final String symbol;

    private final String strategyId;

    private final String marketMakerId;

    private final UUID orderId;

    private final String matchId;

    private final long tradeSeq;

    private final BigDecimal quantity;

    private final BigDecimal price;

    private final BigDecimal notional;

    private final Instant tradedAt;

    private final Instant createdAt;
    public TurnoverRecord(UUID id, long uid, String accountId, String symbol, String strategyId, String marketMakerId, UUID orderId, String matchId, long tradeSeq, BigDecimal quantity, BigDecimal price, BigDecimal notional, Instant tradedAt, Instant createdAt) {
        id = id == null ? UUID.randomUUID() : id;
        accountId = blankToNull(accountId);
        strategyId = blankToNull(strategyId);
        marketMakerId = blankToNull(marketMakerId);
        quantity = quantity == null ? BigDecimal.ZERO : quantity.abs();
        price = price == null ? BigDecimal.ZERO : price;
        notional = notional == null ? price.multiply(quantity) : notional;
        tradedAt = tradedAt == null ? Instant.now() : tradedAt;
        createdAt = createdAt == null ? Instant.now() : createdAt;
    
        this.id = id;
        this.uid = uid;
        this.accountId = accountId;
        this.symbol = symbol;
        this.strategyId = strategyId;
        this.marketMakerId = marketMakerId;
        this.orderId = orderId;
        this.matchId = matchId;
        this.tradeSeq = tradeSeq;
        this.quantity = quantity;
        this.price = price;
        this.notional = notional;
        this.tradedAt = tradedAt;
        this.createdAt = createdAt;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public UUID id() {
        return id;
    }

    public long uid() {
        return uid;
    }

    public String accountId() {
        return accountId;
    }

    public String symbol() {
        return symbol;
    }

    public String strategyId() {
        return strategyId;
    }

    public String marketMakerId() {
        return marketMakerId;
    }

    public UUID orderId() {
        return orderId;
    }

    public String matchId() {
        return matchId;
    }

    public long tradeSeq() {
        return tradeSeq;
    }

    public BigDecimal quantity() {
        return quantity;
    }

    public BigDecimal price() {
        return price;
    }

    public BigDecimal notional() {
        return notional;
    }

    public Instant tradedAt() {
        return tradedAt;
    }

    public Instant createdAt() {
        return createdAt;
    }
}