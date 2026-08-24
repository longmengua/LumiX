package com.lumix.withdrawal.request.reconciliation;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
/** 唯讀 reconciliation output；有 exception 時絕不輸出 P25 signer input。 */
public record WithdrawalRequestReconciliationReport(List<WithdrawalReconciliationException> exceptions, Optional<P25WithdrawalSignerInput> signerInput) {
    public WithdrawalRequestReconciliationReport { exceptions = List.copyOf(Objects.requireNonNull(exceptions, "exceptions")); signerInput = Objects.requireNonNull(signerInput, "signerInput"); if (signerInput.isPresent() != exceptions.isEmpty()) throw new IllegalArgumentException("signer input requires a clean reconciliation report"); }
}
