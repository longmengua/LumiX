package com.lumix.deposit.credit;

import java.math.BigInteger;
import java.util.Objects;

/**
 * 交給未來、獨立審核的 ledger adapter 的唯讀封套。
 *
 * <p>此類別刻意不依賴 LedgerPostingRuntimeGate 或 LedgerService，避免 P23-T02 提早取得 append 權限。</p>
 */
public record DepositCreditLedgerHandoff(
        DepositCreditDecisionRecord decisionRecord,
        BigInteger atomicAmount
) {

    public DepositCreditLedgerHandoff {
        decisionRecord = Objects.requireNonNull(decisionRecord, "decisionRecord must not be null");
        atomicAmount = Objects.requireNonNull(atomicAmount, "atomicAmount must not be null");
        if (!decisionRecord.decision().eligibleForFutureHandoff() || atomicAmount.signum() <= 0) {
            throw new IllegalArgumentException("ledger handoff requires eligible decision and positive atomic amount");
        }
    }
}
