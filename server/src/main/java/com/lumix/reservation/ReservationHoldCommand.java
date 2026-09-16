package com.lumix.reservation;

import java.math.BigDecimal;
import java.util.Objects;

/** 真實資產 reservation hold 的內部 command；不對 browser 暴露。 */
public record ReservationHoldCommand(
        String reservationId, String accountId, String assetSymbol, String businessReferenceId,
        BigDecimal amount, String requestId, String idempotencyKey, String actorId
) {
    public ReservationHoldCommand {
        reservationId = required(reservationId, "reservationId", 64);
        accountId = required(accountId, "accountId", 64);
        assetSymbol = required(assetSymbol, "assetSymbol", 32);
        businessReferenceId = required(businessReferenceId, "businessReferenceId", 128);
        requestId = required(requestId, "requestId", 64);
        idempotencyKey = required(idempotencyKey, "idempotencyKey", 128);
        actorId = required(actorId, "actorId", 64);
        Objects.requireNonNull(amount, "amount must not be null");
        if (amount.signum() <= 0 || amount.scale() > 18) throw new IllegalArgumentException("amount must be positive and precise");
    }
    private static String required(String value, String field, int max) {
        Objects.requireNonNull(value, field + " must not be null");
        String normalized = value.trim();
        if (normalized.isEmpty() || normalized.length() > max) throw new IllegalArgumentException(field + " is invalid");
        return normalized;
    }
}
