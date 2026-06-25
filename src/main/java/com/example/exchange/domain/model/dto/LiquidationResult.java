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
public class LiquidationResult {

    private final long uid;

    private final String symbol;

    private final boolean liquidated;

    private final BigDecimal markPrice;

    private final BigDecimal maintenanceMargin;

    private final BigDecimal equity;

    private final BigDecimal closedQty;

    private final BigDecimal realizedPnl;

    private final BigDecimal insuranceFundCovered;

    private final BigDecimal adlCovered;

    private final String liquidationId;

    private final Instant ts;
    public LiquidationResult(long uid, String symbol, boolean liquidated, BigDecimal markPrice, BigDecimal maintenanceMargin, BigDecimal equity, BigDecimal closedQty, BigDecimal realizedPnl, BigDecimal insuranceFundCovered, BigDecimal adlCovered, String liquidationId, Instant ts) {
        this.uid = uid;
        this.symbol = symbol;
        this.liquidated = liquidated;
        this.markPrice = markPrice;
        this.maintenanceMargin = maintenanceMargin;
        this.equity = equity;
        this.closedQty = closedQty;
        this.realizedPnl = realizedPnl;
        this.insuranceFundCovered = insuranceFundCovered;
        this.adlCovered = adlCovered;
        this.liquidationId = liquidationId;
        this.ts = ts;
    }

    public long uid() {
        return uid;
    }

    public String symbol() {
        return symbol;
    }

    public boolean liquidated() {
        return liquidated;
    }

    public BigDecimal markPrice() {
        return markPrice;
    }

    public BigDecimal maintenanceMargin() {
        return maintenanceMargin;
    }

    public BigDecimal equity() {
        return equity;
    }

    public BigDecimal closedQty() {
        return closedQty;
    }

    public BigDecimal realizedPnl() {
        return realizedPnl;
    }

    public BigDecimal insuranceFundCovered() {
        return insuranceFundCovered;
    }

    public BigDecimal adlCovered() {
        return adlCovered;
    }

    public String liquidationId() {
        return liquidationId;
    }

    public Instant ts() {
        return ts;
    }
}