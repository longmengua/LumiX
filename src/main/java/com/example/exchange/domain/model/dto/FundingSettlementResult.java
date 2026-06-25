/*
 * 檔案用途：領域 DTO，於服務、事件與介面層之間傳遞結構化資料。
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
public class FundingSettlementResult {

    private final long uid;

    private final String symbol;

    private final BigDecimal markPrice;

    private final BigDecimal fundingRate;

    private final BigDecimal cashflow;

    private final String settlementId;

    private final boolean settled;

    private final Instant ts;
    public FundingSettlementResult(long uid, String symbol, BigDecimal markPrice, BigDecimal fundingRate, BigDecimal cashflow, String settlementId, boolean settled, Instant ts) {
        this.uid = uid;
        this.symbol = symbol;
        this.markPrice = markPrice;
        this.fundingRate = fundingRate;
        this.cashflow = cashflow;
        this.settlementId = settlementId;
        this.settled = settled;
        this.ts = ts;
    }

    public long uid() {
        return uid;
    }

    public String symbol() {
        return symbol;
    }

    public BigDecimal markPrice() {
        return markPrice;
    }

    public BigDecimal fundingRate() {
        return fundingRate;
    }

    public BigDecimal cashflow() {
        return cashflow;
    }

    public String settlementId() {
        return settlementId;
    }

    public boolean settled() {
        return settled;
    }

    public Instant ts() {
        return ts;
    }
}