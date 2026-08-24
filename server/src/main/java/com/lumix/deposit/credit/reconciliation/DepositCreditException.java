package com.lumix.deposit.credit.reconciliation;

import com.lumix.deposit.credit.DepositCreditIdempotencyKey;
import java.util.Objects;

/**
 * 只能供未來 exception queue/audit 使用的 immutable discrepancy；沒有自動修復語意。
 */
public record DepositCreditException(DepositCreditIdempotencyKey idempotencyKey, DepositCreditExceptionCode code) {

    public DepositCreditException {
        idempotencyKey = Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null");
        code = Objects.requireNonNull(code, "code must not be null");
    }
}
