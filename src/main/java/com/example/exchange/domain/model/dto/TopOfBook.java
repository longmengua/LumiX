/*
 * 檔案用途：領域 DTO，於服務、事件與介面層之間傳遞結構化資料。
 */
package com.example.exchange.domain.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

import java.math.BigDecimal;

/**
 * Top-of-Book 結構。
 *
 * - 僅包含最優買/賣價，若簿為空則可能為 null。
 */
@Data
@Builder
@Jacksonized()
@JsonIgnoreProperties(ignoreUnknown = true)
public class TopOfBook {
    private BigDecimal bestBid; // 最優買價
    private BigDecimal bestAsk; // 最優賣價
}
