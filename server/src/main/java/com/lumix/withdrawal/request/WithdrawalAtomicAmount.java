package com.lumix.withdrawal.request;

import java.math.BigInteger;
import java.util.Objects;

/**
 * 提款請求的原子整數數量；資產精度與上限由 caller 明確提供，絕不使用浮點數。
 */
public record WithdrawalAtomicAmount(BigInteger atoms) {

    public WithdrawalAtomicAmount {
        atoms = Objects.requireNonNull(atoms, "atoms must not be null");
        if (atoms.signum() <= 0) {
            throw new IllegalArgumentException("withdrawal atomic amount must be positive");
        }
    }
}
