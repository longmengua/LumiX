package com.lumix.reservation;

import java.math.BigDecimal;
import java.util.Objects;

/** 真實資產 reservation hold 的內部 command；不對 browser 暴露。 */
public record ReservationHoldCommand(
        String reservationId, String accountId, String assetSymbol, String businessReferenceId,
        BigDecimal amount, String requestId, String idempotencyKey, String actorId,
        String businessReferenceType, String reservationType
) {
    /** 保留舊呼叫端的相容建構式；新提款流程必須明確指定 WITHDRAWAL/WITHDRAWAL_HOLD。 */
    public ReservationHoldCommand(String reservationId, String accountId, String assetSymbol, String businessReferenceId,
                                  BigDecimal amount, String requestId, String idempotencyKey, String actorId) {
        this(reservationId, accountId, assetSymbol, businessReferenceId, amount, requestId, idempotencyKey, actorId,
                "ADJUSTMENT", "ADMIN_HOLD");
    }
    public ReservationHoldCommand {
        reservationId = required(reservationId, "reservationId", 64);
        accountId = required(accountId, "accountId", 64);
        assetSymbol = required(assetSymbol, "assetSymbol", 32);
        businessReferenceId = required(businessReferenceId, "businessReferenceId", 128);
        requestId = required(requestId, "requestId", 64);
        idempotencyKey = required(idempotencyKey, "idempotencyKey", 128);
        actorId = required(actorId, "actorId", 64);
        businessReferenceType = required(businessReferenceType, "businessReferenceType", 16);
        reservationType = required(reservationType, "reservationType", 24);
        if (!java.util.Set.of("ORDER", "WITHDRAWAL", "SETTLEMENT", "ADJUSTMENT").contains(businessReferenceType)) {
            throw new IllegalArgumentException("businessReferenceType is unsupported");
        }
        if (!java.util.Set.of("ORDER_HOLD", "WITHDRAWAL_HOLD", "SETTLEMENT_HOLD", "ADMIN_HOLD").contains(reservationType)) {
            throw new IllegalArgumentException("reservationType is unsupported");
        }
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
