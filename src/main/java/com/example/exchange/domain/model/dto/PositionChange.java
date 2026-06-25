/*
 * 檔案用途：領域 DTO，於服務、事件與介面層之間傳遞結構化資料。
 */
package com.example.exchange.domain.model.dto;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;


/**
 * 單筆成交套用到持倉後產生的變動。
 */
@Data
@Builder
@Jacksonized
public class PositionChange {

    private final BigDecimal oldQty;

    private final BigDecimal newQty;

    private final BigDecimal oldEntryPrice;

    private final BigDecimal newEntryPrice;

    private final BigDecimal realizedPnl;
    public PositionChange(BigDecimal oldQty, BigDecimal newQty, BigDecimal oldEntryPrice, BigDecimal newEntryPrice, BigDecimal realizedPnl) {
        this.oldQty = oldQty;
        this.newQty = newQty;
        this.oldEntryPrice = oldEntryPrice;
        this.newEntryPrice = newEntryPrice;
        this.realizedPnl = realizedPnl;
    }

    public boolean hasRealizedPnl() {
        return realizedPnl != null && realizedPnl.signum() != 0;
    }

    public BigDecimal oldQty() {
        return oldQty;
    }

    public BigDecimal newQty() {
        return newQty;
    }

    public BigDecimal oldEntryPrice() {
        return oldEntryPrice;
    }

    public BigDecimal newEntryPrice() {
        return newEntryPrice;
    }

    public BigDecimal realizedPnl() {
        return realizedPnl;
    }
}