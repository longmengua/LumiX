/*
 * 檔案用途：領域 DTO，於服務、事件與介面層之間傳遞結構化資料。
 */
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
