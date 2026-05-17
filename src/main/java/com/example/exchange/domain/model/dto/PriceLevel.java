/*
 * 檔案用途：領域 DTO，於服務、事件與介面層之間傳遞結構化資料。
 */
package com.example.exchange.domain.model.dto;

import java.math.BigDecimal;

/**
 * 簡單價量結構：某個 price 的聚合數量 qty
 */
public record PriceLevel(BigDecimal price, BigDecimal qty) {}
