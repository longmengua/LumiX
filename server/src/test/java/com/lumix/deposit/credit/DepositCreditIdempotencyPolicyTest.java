package com.lumix.deposit.credit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.lumix.account.AssetSymbol;
import com.lumix.account.UserId;
import com.lumix.deposit.address.DepositAddress;
import com.lumix.deposit.address.DepositAddressFormat;
import com.lumix.deposit.address.DepositAddressLifecycle;
import com.lumix.deposit.address.DepositAddressOwnership;
import com.lumix.deposit.address.DepositNetwork;
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
import java.util.Map;
import org.junit.jupiter.api.Test;

class DepositCreditIdempotencyPolicyTest {

    private final DepositCreditIdempotencyPolicy idempotency = new DepositCreditIdempotencyPolicy();
    private final DepositCreditLedgerHandoffPolicy handoff = new DepositCreditLedgerHandoffPolicy();

    @Test
    void duplicateAndConcurrentRetryKeepOriginalRecordButConflictingPayloadFailsClosed() {
        DepositCreditDecisionRecord original = record("10", DepositCreditDecisionReason.ELIGIBLE_FOR_FUTURE_HANDOFF);
        DepositCreditIdempotencyResult first = idempotency.evaluate(original, Map.of());
        DepositCreditIdempotencyResult retry = idempotency.evaluate(record("10", DepositCreditDecisionReason.ELIGIBLE_FOR_FUTURE_HANDOFF),
                Map.of(original.idempotencyKey(), original));
        DepositCreditIdempotencyResult conflict = idempotency.evaluate(record("11", DepositCreditDecisionReason.ELIGIBLE_FOR_FUTURE_HANDOFF),
                Map.of(original.idempotencyKey(), original));

        assertEquals(DepositCreditIdempotencyDecision.NEW_RECORD, first.decision());
        assertEquals(DepositCreditIdempotencyDecision.DUPLICATE_REPLAY, retry.decision());
        assertEquals(original, retry.effectiveRecord());
        assertEquals(DepositCreditIdempotencyDecision.CONFLICTING_PAYLOAD_REJECTED, conflict.decision());
        assertEquals(original, conflict.effectiveRecord());
    }

    @Test
    void onlyNewEligibleRecordCanPrepareBoundedFutureLedgerHandoff() {
        DepositCreditDecisionRecord eligible = record("10", DepositCreditDecisionReason.ELIGIBLE_FOR_FUTURE_HANDOFF);
        DepositCreditIdempotencyResult newResult = idempotency.evaluate(eligible, Map.of());
        DepositCreditIdempotencyResult ineligible = idempotency.evaluate(
                record("10", DepositCreditDecisionReason.INSUFFICIENT_CONFIRMATIONS), Map.of());

        assertEquals(BigInteger.TEN, handoff.prepare(newResult,
                new DepositCreditLedgerHandoffBoundary.BigIntegerLimit(BigInteger.TEN)).atomicAmount());
        assertThrows(IllegalStateException.class, () -> handoff.prepare(ineligible,
                new DepositCreditLedgerHandoffBoundary.BigIntegerLimit(BigInteger.TEN)));
        assertThrows(IllegalArgumentException.class, () -> handoff.prepare(newResult,
                new DepositCreditLedgerHandoffBoundary.BigIntegerLimit(BigInteger.valueOf(9))));
    }

    private DepositCreditDecisionRecord record(String amount, DepositCreditDecisionReason reason) {
        DepositNetwork network = new DepositNetwork("ETH_MAINNET", DepositAddressFormat.EVM_HEX);
        DepositCreditPolicyVersion version = new DepositCreditPolicyVersion("deposit-policy-v1");
        DepositAddress address = DepositAddress.from("0xabcdef0000000000000000000000000000000000", network);
        DepositChainObservation observation = new DepositChainObservation(
                new DepositObservationIdentity(network, new ChainReferenceId("tx-1"), new ChainEventIndex(0)),
                new ChainBlockReference(100, new ChainReferenceId("block-100")), address, new AssetSymbol("USDT"),
                new DepositAtomicAmount(new BigInteger(amount)), new ObservationFinality(6, true));
        DepositCreditCandidate candidate = new DepositCreditCandidate(new P23DepositHandoffCandidate(observation,
                new DepositObservationFinalityState(observation.identity(), observation.block(), 6,
                        DepositObservationLifecycle.FINALITY_THRESHOLD_MET)),
                new DepositAddressOwnership(new UserId("user-a"), new AssetSymbol("USDT"), network, address,
                        DepositAddressLifecycle.ACTIVE), version);
        return idempotency.record(candidate, new DepositCreditDecision(reason, version));
    }
}
