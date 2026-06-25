/*
 * 檔案用途：領域 DTO，表示 mark/index price oracle 最新快照。
 */
package com.example.exchange.domain.model.dto;

import java.math.BigDecimal;
import java.time.Instant;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;


@Data
@Builder
@Jacksonized
public class MarkPriceSnapshot {

    private final String symbol;

    private final BigDecimal markPrice;

    private final BigDecimal indexPrice;

    private final String source;

    private final Instant updatedAt;

    private final boolean stale;
    public MarkPriceSnapshot(String symbol, BigDecimal markPrice, BigDecimal indexPrice, String source, Instant updatedAt, boolean stale) {
        this.symbol = symbol;
        this.markPrice = markPrice;
        this.indexPrice = indexPrice;
        this.source = source;
        this.updatedAt = updatedAt;
        this.stale = stale;
    }

    public String symbol() {
        return symbol;
    }

    public BigDecimal markPrice() {
        return markPrice;
    }

    public BigDecimal indexPrice() {
        return indexPrice;
    }

    public String source() {
        return source;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public boolean stale() {
        return stale;
    }
}