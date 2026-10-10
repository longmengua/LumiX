package com.lumix.withdrawal.custody;

import java.util.Objects;
import java.util.Optional;

/** custody 拒絕結果不提供可被誤認為鏈上交易的 reference。 */
public record CustodyWithdrawalResult(CustodyWithdrawalStatus status, Optional<String> externalReference) {
    public CustodyWithdrawalResult {
        status = Objects.requireNonNull(status, "status must not be null"); externalReference = Objects.requireNonNull(externalReference, "externalReference").map(String::trim);
        if (status == CustodyWithdrawalStatus.REJECTED_DISABLED && externalReference.isPresent()) throw new IllegalArgumentException("disabled custody must not return an external reference");
    }
}
