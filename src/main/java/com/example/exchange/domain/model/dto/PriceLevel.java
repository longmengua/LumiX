package com.example.exchange.domain.model.dto;

import java.math.BigDecimal;

/**
 * 簡單價量結構：某個 price 的聚合數量 qty
 */
public record PriceLevel(BigDecimal price, BigDecimal qty) {}
