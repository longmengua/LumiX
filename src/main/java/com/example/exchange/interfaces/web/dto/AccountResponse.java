/*
 * File purpose: Stable HTTP response for account balances without exposing the mutable domain object.
 */
package com.example.exchange.interfaces.web.dto;

import com.example.exchange.domain.model.entity.Account;

import java.math.BigDecimal;
import java.time.Instant;

public record AccountResponse(
        long uid,
        BigDecimal balance,
        BigDecimal available,
        BigDecimal frozen,
        BigDecimal orderHold,
        BigDecimal positionMargin,
        Instant updatedAt
) {

    /** Maps the internal account aggregate to frontend-friendly balance fields. */
    public static AccountResponse from(Account account) {
        return new AccountResponse(
                account.uid(),
                account.crossBalance(),
                account.crossAvailable(),
                account.crossHold(),
                account.crossOrderHold(),
                account.crossPositionMargin(),
                account.updatedAt()
        );
    }
}
