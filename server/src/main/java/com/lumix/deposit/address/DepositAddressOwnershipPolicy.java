package com.lumix.deposit.address;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * P22-T01 的 pure ownership uniqueness policy。
 *
 * <p>此類別不配置地址、不修改 map、不接 wallet；caller 必須把 immutable result 交給未來獲批准的 persistence
 * boundary。相同 user 的相同聲明冪等，不同 user 的 network/address 衝突一律 fail closed。</p>
 */
public final class DepositAddressOwnershipPolicy {

    /**
     * 判斷 candidate 是否可登錄。existing ownership 由 caller 顯式傳入，讓測試與未來 replay 都可重現。
     */
    public DepositAddressOwnershipResult evaluate(
            Map<DepositAddressOwnership.NetworkAddressKey, DepositAddressOwnership> existing,
            DepositAddressOwnership candidate
    ) {
        existing = Map.copyOf(Objects.requireNonNull(existing, "existing must not be null"));
        candidate = Objects.requireNonNull(candidate, "candidate must not be null");
        Optional<DepositAddressOwnership> prior = Optional.ofNullable(existing.get(candidate.networkAddressKey()));
        if (prior.isEmpty()) {
            return new DepositAddressOwnershipResult(DepositAddressOwnershipDecision.REGISTERED, candidate);
        }
        if (!prior.orElseThrow().ownerUserId().equals(candidate.ownerUserId())) {
            return new DepositAddressOwnershipResult(DepositAddressOwnershipDecision.CONFLICTING_OWNER_REJECTED, prior.orElseThrow());
        }
        return new DepositAddressOwnershipResult(DepositAddressOwnershipDecision.DUPLICATE_IGNORED, prior.orElseThrow());
    }
}
