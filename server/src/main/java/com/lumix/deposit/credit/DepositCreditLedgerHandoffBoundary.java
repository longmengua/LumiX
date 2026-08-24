package com.lumix.deposit.credit;

/**
 * 未來 ledger adapter 的明確隔離邊界；P23-T02 僅允許產生 handoff，不允許 append。
 */
public interface DepositCreditLedgerHandoffBoundary {

    DepositCreditLedgerHandoff prepare(DepositCreditIdempotencyResult idempotencyResult, BigIntegerLimit maximumAtomicAmount);

    /** 資產精度/overflow policy 已正規化成 atomic 上限，避免 binary floating point。 */
    record BigIntegerLimit(java.math.BigInteger maximum) {
        public BigIntegerLimit {
            maximum = java.util.Objects.requireNonNull(maximum, "maximum must not be null");
            if (maximum.signum() <= 0) {
                throw new IllegalArgumentException("maximum atomic amount must be positive");
            }
        }
    }
}
