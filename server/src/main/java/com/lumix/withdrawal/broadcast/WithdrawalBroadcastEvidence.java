package com.lumix.withdrawal.broadcast;

import java.time.Instant;
import java.util.Objects;

/** 一筆外部 broadcast observation；reference 只用於對帳，不是可呼叫的交易或 provider handle。 */
public record WithdrawalBroadcastEvidence(String attemptId, String signingIntentDigest, String externalReference, WithdrawalBroadcastStatus status, Instant observedAt) {
    public WithdrawalBroadcastEvidence {
        attemptId = required(attemptId, "attemptId"); signingIntentDigest = required(signingIntentDigest, "signingIntentDigest"); externalReference = required(externalReference, "externalReference"); status = Objects.requireNonNull(status, "status"); observedAt = Objects.requireNonNull(observedAt, "observedAt");
        if (!signingIntentDigest.matches("[0-9a-f]{64}")) throw new IllegalArgumentException("signing intent digest must be SHA-256 hex");
    }
    private static String required(String value, String name) { value = Objects.requireNonNull(value, name).trim(); if (value.isEmpty()) throw new IllegalArgumentException(name + " must not be blank"); return value; }
}
