package com.lumix.deposit.credit.reconciliation;

import com.lumix.deposit.credit.DepositCreditDecisionRecord;
import com.lumix.deposit.credit.DepositCreditIdempotencyKey;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * P23-T04 的 pure, fail-closed deposit/ledger/balance evidence reconciler。
 *
 * <p>所有輸入先按 idempotency key canonical sort，因此 replay 不受 caller collection 順序影響；缺失或錯誤 evidence
 * 只進 exception queue，絕不嘗試修復或執行 admin action。</p>
 */
public final class DepositCreditReconciliationPolicy {

    public DepositCreditReconciliationReport evaluate(
            Collection<DepositCreditDecisionRecord> records,
            Map<DepositCreditIdempotencyKey, DepositLedgerEvidence> ledgerEvidence,
            Map<DepositCreditIdempotencyKey, DepositBalanceEvidence> balanceEvidence
    ) {
        List<DepositCreditDecisionRecord> ordered = List.copyOf(Objects.requireNonNull(records, "records must not be null"))
                .stream().sorted(Comparator.comparing(record -> record.idempotencyKey().value())).toList();
        ledgerEvidence = Map.copyOf(Objects.requireNonNull(ledgerEvidence, "ledgerEvidence must not be null"));
        balanceEvidence = Map.copyOf(Objects.requireNonNull(balanceEvidence, "balanceEvidence must not be null"));
        List<DepositCreditException> exceptions = new ArrayList<>();

        for (DepositCreditDecisionRecord record : ordered) {
            DepositCreditIdempotencyKey key = record.idempotencyKey();
            if (!record.decision().eligibleForFutureHandoff()) {
                if (ledgerEvidence.containsKey(key) || balanceEvidence.containsKey(key)) {
                    exceptions.add(new DepositCreditException(key, DepositCreditExceptionCode.INELIGIBLE_RECORD_HAS_POSTING_EVIDENCE));
                }
                continue;
            }
            if (!ledgerEvidence.containsKey(key)) {
                exceptions.add(new DepositCreditException(key, DepositCreditExceptionCode.MISSING_LEDGER_EVIDENCE));
            }
            DepositBalanceEvidence balance = balanceEvidence.get(key);
            if (balance == null) {
                exceptions.add(new DepositCreditException(key, DepositCreditExceptionCode.MISSING_BALANCE_EVIDENCE));
                continue;
            }
            if (!balance.ownerUserId().equals(record.candidate().ownership().ownerUserId())
                    || !balance.asset().equals(record.candidate().observationEvidence().observation().asset())) {
                exceptions.add(new DepositCreditException(key, DepositCreditExceptionCode.OWNER_OR_ASSET_MISMATCH));
            }
            if (!balance.atomicAmount().equals(record.candidate().observationEvidence().observation().amount().atoms())) {
                exceptions.add(new DepositCreditException(key, DepositCreditExceptionCode.ATOMIC_AMOUNT_MISMATCH));
            }
        }
        return new DepositCreditReconciliationReport(exceptions.isEmpty(), ordered, exceptions);
    }
}
