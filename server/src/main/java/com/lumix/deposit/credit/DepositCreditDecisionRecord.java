package com.lumix.deposit.credit;

import java.util.Objects;

/**
 * 可供未來 persistence/audit 邊界保存的 immutable credit decision evidence。
 *
 * <p>record 本身不是 ledger entry，也不會改變 balance；同一 key 的 payload fingerprint 必須永遠一致。</p>
 */
public record DepositCreditDecisionRecord(
        DepositCreditIdempotencyKey idempotencyKey,
        String payloadFingerprint,
        DepositCreditCandidate candidate,
        DepositCreditDecision decision
) {

    public DepositCreditDecisionRecord {
        idempotencyKey = Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null");
        payloadFingerprint = Objects.requireNonNull(payloadFingerprint, "payloadFingerprint must not be null");
        candidate = Objects.requireNonNull(candidate, "candidate must not be null");
        decision = Objects.requireNonNull(decision, "decision must not be null");
        if (!payloadFingerprint.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("payload fingerprint must be lowercase SHA-256 hex");
        }
    }
}
