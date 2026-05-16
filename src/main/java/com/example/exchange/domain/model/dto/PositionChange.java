package com.example.exchange.domain.model.dto;

import java.math.BigDecimal;

/**
 * 單筆成交套用到持倉後產生的變動。
 */
public record PositionChange(
        BigDecimal oldQty,
        BigDecimal newQty,
        BigDecimal oldEntryPrice,
        BigDecimal newEntryPrice,
        BigDecimal realizedPnl
) {
    public boolean hasRealizedPnl() {
        return realizedPnl != null && realizedPnl.signum() != 0;
    }
}
