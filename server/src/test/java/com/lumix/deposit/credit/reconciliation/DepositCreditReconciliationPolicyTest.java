package com.lumix.deposit.credit.reconciliation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lumix.account.AssetSymbol;
import com.lumix.account.UserId;
import com.lumix.deposit.address.DepositAddress;
import com.lumix.deposit.address.DepositAddressFormat;
import com.lumix.deposit.address.DepositAddressLifecycle;
import com.lumix.deposit.address.DepositAddressOwnership;
import com.lumix.deposit.address.DepositNetwork;
import com.lumix.deposit.credit.DepositCreditCandidate;
import com.lumix.deposit.credit.DepositCreditDecision;
import com.lumix.deposit.credit.DepositCreditDecisionReason;
import com.lumix.deposit.credit.DepositCreditDecisionRecord;
import com.lumix.deposit.credit.DepositCreditIdempotencyPolicy;
import com.lumix.deposit.credit.DepositCreditPolicyVersion;
import com.lumix.deposit.observation.ChainBlockReference;
import com.lumix.deposit.observation.ChainEventIndex;
import com.lumix.deposit.observation.ChainReferenceId;
import com.lumix.deposit.observation.DepositAtomicAmount;
import com.lumix.deposit.observation.DepositChainObservation;
import com.lumix.deposit.observation.DepositObservationIdentity;
import com.lumix.deposit.observation.ObservationFinality;
import com.lumix.deposit.observation.finality.DepositObservationFinalityState;
import com.lumix.deposit.observation.finality.DepositObservationLifecycle;
import com.lumix.deposit.observation.reconciliation.P23DepositHandoffCandidate;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DepositCreditReconciliationPolicyTest {

    private final DepositCreditReconciliationPolicy policy = new DepositCreditReconciliationPolicy();

    @Test
    void completeEvidenceReplaysDeterministicallyForAuditExport() {
        DepositCreditDecisionRecord first = record("tx-a", "10");
        DepositCreditDecisionRecord second = record("tx-b", "11");
        Map<com.lumix.deposit.credit.DepositCreditIdempotencyKey, DepositLedgerEvidence> ledger = Map.of(
                first.idempotencyKey(), new DepositLedgerEvidence(first.idempotencyKey(), "journal-a"),
                second.idempotencyKey(), new DepositLedgerEvidence(second.idempotencyKey(), "journal-b"));
        Map<com.lumix.deposit.credit.DepositCreditIdempotencyKey, DepositBalanceEvidence> balance = Map.of(
                first.idempotencyKey(), balance(first), second.idempotencyKey(), balance(second));

        DepositCreditReconciliationReport one = policy.evaluate(List.of(second, first), ledger, balance);
        DepositCreditReconciliationReport two = policy.evaluate(List.of(first, second), ledger, balance);

        assertTrue(one.reconciled());
        assertEquals(one.auditExportInput(), two.auditExportInput());
    }

    @Test
    void missingOrMismatchedEvidenceProducesFailClosedExceptions() {
        DepositCreditDecisionRecord record = record("tx-a", "10");
        DepositCreditReconciliationReport missing = policy.evaluate(List.of(record), Map.of(), Map.of());
        DepositCreditReconciliationReport mismatch = policy.evaluate(List.of(record),
                Map.of(record.idempotencyKey(), new DepositLedgerEvidence(record.idempotencyKey(), "journal-a")),
                Map.of(record.idempotencyKey(), new DepositBalanceEvidence(record.idempotencyKey(), new UserId("user-a"),
                        new AssetSymbol("USDT"), BigInteger.valueOf(9))));

        assertFalse(missing.reconciled());
        assertEquals(2, missing.exceptions().size());
        assertFalse(mismatch.reconciled());
        assertEquals(DepositCreditExceptionCode.ATOMIC_AMOUNT_MISMATCH, mismatch.exceptions().getFirst().code());
    }

    private static DepositBalanceEvidence balance(DepositCreditDecisionRecord record) {
        return new DepositBalanceEvidence(record.idempotencyKey(), record.candidate().ownership().ownerUserId(),
                record.candidate().observationEvidence().observation().asset(),
                record.candidate().observationEvidence().observation().amount().atoms());
    }

    private static DepositCreditDecisionRecord record(String tx, String amount) {
        DepositNetwork network = new DepositNetwork("ETH_MAINNET", DepositAddressFormat.EVM_HEX);
        DepositCreditPolicyVersion version = new DepositCreditPolicyVersion("deposit-policy-v1");
        DepositAddress address = DepositAddress.from("0xabcdef0000000000000000000000000000000000", network);
        DepositChainObservation observation = new DepositChainObservation(
                new DepositObservationIdentity(network, new ChainReferenceId(tx), new ChainEventIndex(0)),
                new ChainBlockReference(100, new ChainReferenceId("block-100")), address, new AssetSymbol("USDT"),
                new DepositAtomicAmount(new BigInteger(amount)), new ObservationFinality(6, true));
        DepositCreditCandidate candidate = new DepositCreditCandidate(new P23DepositHandoffCandidate(observation,
                new DepositObservationFinalityState(observation.identity(), observation.block(), 6,
                        DepositObservationLifecycle.FINALITY_THRESHOLD_MET)),
                new DepositAddressOwnership(new UserId("user-a"), new AssetSymbol("USDT"), network, address,
                        DepositAddressLifecycle.ACTIVE), version);
        return new DepositCreditIdempotencyPolicy().record(candidate,
                new DepositCreditDecision(DepositCreditDecisionReason.ELIGIBLE_FOR_FUTURE_HANDOFF, version));
    }
}
