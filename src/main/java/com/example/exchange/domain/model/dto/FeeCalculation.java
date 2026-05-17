/*
 * 檔案用途：領域 DTO，於服務、事件與介面層之間傳遞結構化資料。
 */
package com.example.exchange.domain.model.dto;

import java.math.BigDecimal;

public record FeeCalculation(
        BigDecimal feeRate,
        BigDecimal fee,
        BigDecimal rebateRate,
        BigDecimal rebate
) {}
