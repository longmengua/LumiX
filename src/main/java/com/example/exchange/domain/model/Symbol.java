package com.example.exchange.domain.model;

import lombok.*;
import lombok.extern.jackson.Jacksonized;

/**
 * 交易對值物件
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Jacksonized
public class Symbol {
    private String base;
    private String quote;
    private int priceScale;
    private int qtyScale;
    public String code() { return base + quote; }
}

