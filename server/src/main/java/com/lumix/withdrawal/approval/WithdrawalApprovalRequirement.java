package com.lumix.withdrawal.approval;

import com.lumix.withdrawal.request.WithdrawalAtomicAmount;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;

/** 每筆審核的版本化 requirement；限額與期限都由呼叫端顯式傳入，避免 policy 猜測營運規則。 */
public record WithdrawalApprovalRequirement(Set<WithdrawalApprovalRole> requiredRoles, WithdrawalAtomicAmount maximumAtomicAmount, Instant expiresAt) {
    public WithdrawalApprovalRequirement {
        requiredRoles = Set.copyOf(Objects.requireNonNull(requiredRoles, "requiredRoles"));
        maximumAtomicAmount = Objects.requireNonNull(maximumAtomicAmount, "maximumAtomicAmount");
        expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
        if (requiredRoles.isEmpty()) {
            throw new IllegalArgumentException("at least one approval role is required");
        }
    }
}
