package com.lumix.deposit.observation;

import java.math.BigInteger;
import java.util.Objects;

/**
 * 入金觀測到的資產最小單位整數。
 *
 * <p>precision 由資產/network contract 管理；此層僅保存不可為零或負的原子數量，避免 binary floating point。
 */
public record DepositAtomicAmount(BigInteger atoms) {

    public DepositAtomicAmount {
        atoms = Objects.requireNonNull(atoms, "atoms must not be null");
        if (atoms.signum() <= 0) {
            throw new IllegalArgumentException("deposit atomic amount must be positive");
        }
    }
}
