package com.lumix.withdrawal.request.eligibility;

import com.lumix.account.AssetSymbol;
import com.lumix.account.UserId;
import java.math.BigInteger;
import java.util.Objects;

/** caller 提供的唯讀 available balance evidence；不代表 hold 已建立。 */
public record WithdrawalAvailableBalanceEvidence(UserId ownerUserId, AssetSymbol asset, BigInteger availableAtomicAmount) {
    public WithdrawalAvailableBalanceEvidence {
        ownerUserId = Objects.requireNonNull(ownerUserId, "ownerUserId must not be null");
        asset = Objects.requireNonNull(asset, "asset must not be null");
        availableAtomicAmount = Objects.requireNonNull(availableAtomicAmount, "availableAtomicAmount must not be null");
        if (availableAtomicAmount.signum() < 0) {
            throw new IllegalArgumentException("available balance must not be negative");
        }
    }
}
