/*
 * File purpose: Admin response DTO exposing fee-change audit records without leaking internal JPA types.
 */
package com.example.exchange.interfaces.web.dto;

import com.example.exchange.domain.model.entity.FeeConfigChangeRecord;

import java.math.BigDecimal;
import java.time.Instant;

public record FeeConfigChangeResponse(
        String id,
        String symbol,
        BigDecimal oldMakerFeeRate,
        BigDecimal oldTakerFeeRate,
        BigDecimal newMakerFeeRate,
        BigDecimal newTakerFeeRate,
        String operatorId,
        String reason,
        String requestId,
        Instant effectiveAt,
        Instant changedAt
) {

    public static FeeConfigChangeResponse from(FeeConfigChangeRecord record) {
        return new FeeConfigChangeResponse(
                record.getId(),
                record.getSymbol(),
                record.getOldMakerFeeRate(),
                record.getOldTakerFeeRate(),
                record.getNewMakerFeeRate(),
                record.getNewTakerFeeRate(),
                record.getOperatorId(),
                record.getReason(),
                record.getRequestId(),
                record.getEffectiveAt(),
                record.getChangedAt()
        );
    }
}
