package com.lumix.deposit.credit.reconciliation;

import com.lumix.deposit.credit.DepositCreditDecisionRecord;
import java.util.List;
import java.util.Objects;

/**
 * 可重放、可匯出的 reconciliation evidence；有任何 exception 時 reconciled 必須為 false。
 */
public record DepositCreditReconciliationReport(
        boolean reconciled,
        List<DepositCreditDecisionRecord> auditExportInput,
        List<DepositCreditException> exceptions
) {

    public DepositCreditReconciliationReport {
        auditExportInput = List.copyOf(Objects.requireNonNull(auditExportInput, "auditExportInput must not be null"));
        exceptions = List.copyOf(Objects.requireNonNull(exceptions, "exceptions must not be null"));
        if (reconciled != exceptions.isEmpty()) {
            throw new IllegalArgumentException("reconciliation flag must exactly reflect exception absence");
        }
    }
}
