package com.lumix.withdrawal.request.eligibility;

import com.lumix.withdrawal.request.WithdrawalRequest;
import java.math.BigInteger;
import java.util.Objects;

/** 未來 reservation boundary 的純封套，不建立、capture 或 release hold。 */
public record WithdrawalHoldHandoff(WithdrawalRequest request, WithdrawalFeeQuote feeQuote, BigInteger requiredAtomicAmount) {
    public WithdrawalHoldHandoff {
        request = Objects.requireNonNull(request, "request must not be null");
        feeQuote = Objects.requireNonNull(feeQuote, "feeQuote must not be null");
        requiredAtomicAmount = Objects.requireNonNull(requiredAtomicAmount, "requiredAtomicAmount must not be null");
        if (requiredAtomicAmount.signum() <= 0) {
            throw new IllegalArgumentException("hold handoff amount must be positive");
        }
    }
}
