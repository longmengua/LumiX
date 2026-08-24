package com.lumix.deposit.credit.reconciliation;

import com.lumix.deposit.credit.DepositCreditIdempotencyKey;
import java.util.Objects;

/**
 * 外部 ledger append 結果的唯讀稽核引用，不直接暴露或操作 ledger row。
 */
public record DepositLedgerEvidence(DepositCreditIdempotencyKey idempotencyKey, String journalEvidenceId) {

    public DepositLedgerEvidence {
        idempotencyKey = Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null");
        journalEvidenceId = Objects.requireNonNull(journalEvidenceId, "journalEvidenceId must not be null").trim();
        if (journalEvidenceId.isBlank()) {
            throw new IllegalArgumentException("journal evidence id must not be blank");
        }
    }
}
