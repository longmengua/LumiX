package com.lumix.deposit.address;

import java.util.Objects;

/** 不寫入任何 storage 的 immutable policy result。 */
public record DepositAddressOwnershipResult(
        DepositAddressOwnershipDecision decision,
        DepositAddressOwnership ownership
) {
    public DepositAddressOwnershipResult {
        decision = Objects.requireNonNull(decision, "decision must not be null");
        ownership = Objects.requireNonNull(ownership, "ownership must not be null");
    }
}
