package com.example.java21_OLAP.domain.model;

/**
 * Symbol (交易對)
 *
 * - base: BTC
 * - quote: USDT
 * - priceScale: 小數精度
 * - qtyScale: 數量精度
 */
public record Symbol(String base, String quote, int priceScale, int qtyScale) {
    public String code() { return base + quote; }
}
