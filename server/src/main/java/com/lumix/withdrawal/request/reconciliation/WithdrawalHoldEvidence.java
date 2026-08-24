package com.lumix.withdrawal.request.reconciliation;

import com.lumix.withdrawal.request.WithdrawalRequestId;
import java.util.Objects;

/** 未來 reservation adapter 提供的 immutable hold reference，不建立或釋放 hold。 */
public record WithdrawalHoldEvidence(WithdrawalRequestId requestId, String holdEvidenceId) {
    public WithdrawalHoldEvidence { requestId = Objects.requireNonNull(requestId, "requestId"); holdEvidenceId = Objects.requireNonNull(holdEvidenceId, "holdEvidenceId").trim(); if (holdEvidenceId.isBlank()) throw new IllegalArgumentException("hold evidence id must not be blank"); }
}
