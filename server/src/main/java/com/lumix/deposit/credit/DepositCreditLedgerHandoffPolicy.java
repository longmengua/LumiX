package com.lumix.deposit.credit;

import java.math.BigInteger;
import java.util.Objects;

/**
 * 唯讀 handoff preparer；只接受 NEW_RECORD，duplicate/conflict/ineligible 都不會重新交給 ledger。
 */
public final class DepositCreditLedgerHandoffPolicy implements DepositCreditLedgerHandoffBoundary {

    @Override
    public DepositCreditLedgerHandoff prepare(
            DepositCreditIdempotencyResult idempotencyResult,
            BigIntegerLimit maximumAtomicAmount
    ) {
        idempotencyResult = Objects.requireNonNull(idempotencyResult, "idempotencyResult must not be null");
        maximumAtomicAmount = Objects.requireNonNull(maximumAtomicAmount, "maximumAtomicAmount must not be null");
        if (idempotencyResult.decision() != DepositCreditIdempotencyDecision.NEW_RECORD) {
            throw new IllegalStateException("only a new eligible decision record may enter future ledger handoff");
        }
        BigInteger amount = idempotencyResult.effectiveRecord().candidate().observationEvidence().observation().amount().atoms();
        if (amount.compareTo(maximumAtomicAmount.maximum()) > 0) {
            throw new IllegalArgumentException("deposit atomic amount exceeds configured overflow boundary");
        }
        return new DepositCreditLedgerHandoff(idempotencyResult.effectiveRecord(), amount);
    }
}
