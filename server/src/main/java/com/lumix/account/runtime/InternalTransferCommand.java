package com.lumix.account.runtime;

import java.math.BigDecimal;
import java.util.Objects;

/** 已通過上游 owner authorization 的 internal transfer command；不屬公開 API DTO。 */
public record InternalTransferCommand(String transferId, String ownerUserId, String sourceAccountId,
                                      String destinationAccountId, String assetSymbol, BigDecimal amount,
                                      String requestId, String idempotencyKey, String actorId) {
    public InternalTransferCommand {
        transferId = required(transferId); ownerUserId = required(ownerUserId); sourceAccountId = required(sourceAccountId);
        destinationAccountId = required(destinationAccountId); assetSymbol = required(assetSymbol); requestId = required(requestId);
        idempotencyKey = required(idempotencyKey); actorId = required(actorId);
        Objects.requireNonNull(amount, "amount must not be null");
        if (amount.signum() <= 0 || sourceAccountId.equals(destinationAccountId)) throw new IllegalArgumentException("transfer command is invalid");
    }
    private static String required(String value) { Objects.requireNonNull(value); String normalized = value.trim(); if (normalized.isEmpty()) throw new IllegalArgumentException("transfer identity must not be blank"); return normalized; }
}
