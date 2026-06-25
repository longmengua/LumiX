/*
 * 檔案用途：領域 DTO，於服務、事件與介面層之間傳遞結構化資料。
 */
package com.example.exchange.domain.model.dto;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;


/**
 * 簡單價量結構：某個 price 的聚合數量 qty
 */
@Data
@Builder
@Jacksonized
public class PriceLevel {

    private final BigDecimal price;

    private final BigDecimal qty;
    public PriceLevel(BigDecimal price, BigDecimal qty) {
        this.price = price;
        this.qty = qty;
    }

    public BigDecimal price() {
        return price;
    }

    public BigDecimal qty() {
        return qty;
    }
}