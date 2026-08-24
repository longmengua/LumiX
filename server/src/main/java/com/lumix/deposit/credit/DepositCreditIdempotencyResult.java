package com.lumix.deposit.credit;

import java.util.Objects;
import java.util.Optional;

/**
 * idempotency 評估結果；previous record 若存在，caller 必須保留其原始 evidence 而不能覆寫。
 */
public record DepositCreditIdempotencyResult(
        DepositCreditIdempotencyDecision decision,
        DepositCreditDecisionRecord effectiveRecord,
        Optional<DepositCreditDecisionRecord> previousRecord
) {

    public DepositCreditIdempotencyResult {
        decision = Objects.requireNonNull(decision, "decision must not be null");
        effectiveRecord = Objects.requireNonNull(effectiveRecord, "effectiveRecord must not be null");
        previousRecord = Objects.requireNonNull(previousRecord, "previousRecord must not be null");
        if ((decision == DepositCreditIdempotencyDecision.DUPLICATE_REPLAY
                || decision == DepositCreditIdempotencyDecision.CONFLICTING_PAYLOAD_REJECTED)
                && previousRecord.isEmpty()) {
            throw new IllegalArgumentException("duplicate/conflict decision requires prior immutable record");
        }
    }
}
