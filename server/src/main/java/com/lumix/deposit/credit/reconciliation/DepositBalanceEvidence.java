package com.lumix.deposit.credit.reconciliation;

import com.lumix.account.AssetSymbol;
import com.lumix.account.UserId;
import com.lumix.deposit.credit.DepositCreditIdempotencyKey;
import java.math.BigInteger;
import java.util.Objects;

/**
 * balance projection 對應 credit 的唯讀 evidence；amount 必須仍以 atomic integer 表達。
 */
public record DepositBalanceEvidence(
        DepositCreditIdempotencyKey idempotencyKey,
        UserId ownerUserId,
        AssetSymbol asset,
        BigInteger atomicAmount
) {

    public DepositBalanceEvidence {
        idempotencyKey = Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null");
        ownerUserId = Objects.requireNonNull(ownerUserId, "ownerUserId must not be null");
        asset = Objects.requireNonNull(asset, "asset must not be null");
        atomicAmount = Objects.requireNonNull(atomicAmount, "atomicAmount must not be null");
        if (atomicAmount.signum() <= 0) {
            throw new IllegalArgumentException("balance evidence amount must be positive");
        }
    }
}
