/*
 * 檔案用途：ADL queue 營運告警明細 DTO。
 */
package com.example.exchange.domain.model.dto;

import java.math.BigDecimal;

public record AdlOperationalAlert(
        String alertType,
        String severity,
        String liquidationId,
        long uid,
        String symbol,
        BigDecimal amount,
        String status,
        String owner,
        long ageSeconds,
        String detail
) {
}
