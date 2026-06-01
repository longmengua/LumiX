/*
 * 檔案用途：insurance fund capital movement read model。
 */
package com.example.exchange.domain.model.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record InsuranceFundMovement(
        String movementId,
        String asset,
        String reason,
        String refId,
        BigDecimal amount,
        BigDecimal balanceAfter,
        Instant createdAt
) {
}
