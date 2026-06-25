/*
 * 檔案用途：流水統計 DTO，用於對帳與活動門檻查詢。
 */
package com.example.exchange.domain.model.dto;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;


/**
 * 聚合後的 turnover 結果。
 */
@Data
@Builder
@Jacksonized
public class TurnoverSummary {

    private final long uid;

    private final String symbol;

    private final String strategyId;

    private final String marketMakerId;

    private final long tradeCount;

    private final BigDecimal quantity;

    private final BigDecimal notional;
    public TurnoverSummary(long uid, String symbol, String strategyId, String marketMakerId, long tradeCount, BigDecimal quantity, BigDecimal notional) {
        this.uid = uid;
        this.symbol = symbol;
        this.strategyId = strategyId;
        this.marketMakerId = marketMakerId;
        this.tradeCount = tradeCount;
        this.quantity = quantity;
        this.notional = notional;
    }

    public long uid() {
        return uid;
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

    public long tradeCount() {
        return tradeCount;
    }

    public BigDecimal quantity() {
        return quantity;
    }

    public BigDecimal notional() {
        return notional;
    }
}