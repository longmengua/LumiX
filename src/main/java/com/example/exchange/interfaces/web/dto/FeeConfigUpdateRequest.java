/*
 * File purpose: Admin request DTO for changing maker/taker fee rates on one market.
 */
package com.example.exchange.interfaces.web.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record FeeConfigUpdateRequest(
        BigDecimal makerFeeRate,
        BigDecimal takerFeeRate,
        String operatorId,
        String reason,
        Instant effectiveAt
) {
}
