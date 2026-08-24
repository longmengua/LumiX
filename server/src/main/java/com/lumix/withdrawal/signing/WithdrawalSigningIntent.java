package com.lumix.withdrawal.signing;

import com.lumix.withdrawal.request.reconciliation.P25WithdrawalSignerInput;
import java.util.Objects;

/** 固定 P24 request/hold/audit evidence 的 keyless intent；digest 不是 transaction payload 或簽名。 */
public record WithdrawalSigningIntent(P25WithdrawalSignerInput signerInput, WithdrawalSigningIntentIdempotencyKey idempotencyKey, String intentDigest) {
    public WithdrawalSigningIntent {
        signerInput = Objects.requireNonNull(signerInput, "signerInput");
        idempotencyKey = Objects.requireNonNull(idempotencyKey, "idempotencyKey");
        intentDigest = Objects.requireNonNull(intentDigest, "intentDigest");
        if (!intentDigest.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("intent digest must be SHA-256 hex");
        }
    }
}
