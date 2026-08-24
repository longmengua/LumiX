package com.lumix.deposit.address;

/** pure ownership registry policy 的固定決策，供後續 persistence/observation adapter 保留 idempotency 語意。 */
public enum DepositAddressOwnershipDecision {
    REGISTERED,
    DUPLICATE_IGNORED,
    CONFLICTING_OWNER_REJECTED
}
