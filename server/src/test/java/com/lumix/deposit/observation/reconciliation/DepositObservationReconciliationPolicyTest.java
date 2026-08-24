package com.lumix.deposit.observation.reconciliation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.lumix.account.AssetSymbol;
import com.lumix.deposit.address.DepositAddress;
import com.lumix.deposit.address.DepositAddressFormat;
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
import com.lumix.deposit.observation.finality.ObservationNetworkHealth;
import com.lumix.deposit.observation.finality.ObservationNetworkHealthState;
import java.math.BigInteger;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DepositObservationReconciliationPolicyTest {

    private final DepositObservationReconciliationPolicy policy = new DepositObservationReconciliationPolicy();
    private final DepositNetwork network = new DepositNetwork("ETH_MAINNET", DepositAddressFormat.EVM_HEX);

    @Test
    void inputOrderDoesNotChangeReplayedEvidenceOrP23Handoff() {
        DepositChainObservation first = observation("tx-a", 10);
        DepositChainObservation second = observation("tx-b", 11);
        Map<DepositObservationIdentity, DepositObservationFinalityState> evidence = Map.of(
                first.identity(), state(first, DepositObservationLifecycle.PENDING_CONFIRMATION),
                second.identity(), state(second, DepositObservationLifecycle.FINALITY_THRESHOLD_MET));

        DepositObservationReconciliationReport one = policy.evaluate(network, healthy(), List.of(second, first), evidence);
        DepositObservationReconciliationReport two = policy.evaluate(network, healthy(), List.of(first, second), evidence);

        assertEquals(ObservationReconciliationStatus.RECONCILED, one.status());
        assertEquals(one.evidenceDigest(), two.evidenceDigest());
        assertEquals(1, one.p23Handoff().orElseThrow().candidates().size());
        assertEquals(second.identity(), one.p23Handoff().orElseThrow().candidates().getFirst().observation().identity());
    }

    @Test
    void missingOrConflictingEvidenceAndHaltedNetworkNeverExposeHandoff() {
        DepositChainObservation observation = observation("tx-a", 10);
        DepositObservationReconciliationReport missing = policy.evaluate(network, healthy(), List.of(observation), Map.of());
        DepositObservationReconciliationReport conflicting = policy.evaluate(network, healthy(), List.of(observation), Map.of(
                observation.identity(), new DepositObservationFinalityState(observation.identity(),
                new ChainBlockReference(10, new ChainReferenceId("other-block")), 3, DepositObservationLifecycle.QUARANTINED)));
        DepositObservationReconciliationReport halted = policy.evaluate(network,
                new ObservationNetworkHealth(network, ObservationNetworkHealthState.HALTED_REORG,
                        java.util.Optional.empty(), Instant.parse("2026-08-24T00:00:00Z")),
                List.of(observation), Map.of(observation.identity(), state(observation, DepositObservationLifecycle.FINALITY_THRESHOLD_MET)));

        assertEquals(ObservationReconciliationStatus.MISSING_EVIDENCE, missing.status());
        assertEquals(ObservationReconciliationStatus.CONFLICTING_EVIDENCE, conflicting.status());
        assertEquals(ObservationReconciliationStatus.BLOCKED_NETWORK_HEALTH, halted.status());
        assertFalse(missing.p23Handoff().isPresent());
        assertFalse(conflicting.p23Handoff().isPresent());
        assertFalse(halted.p23Handoff().isPresent());
    }

    private ObservationNetworkHealth healthy() {
        return ObservationNetworkHealth.healthy(network, Instant.parse("2026-08-24T00:00:00Z"));
    }

    private DepositChainObservation observation(String transactionId, long height) {
        return new DepositChainObservation(
                new DepositObservationIdentity(network, new ChainReferenceId(transactionId), new ChainEventIndex(0)),
                new ChainBlockReference(height, new ChainReferenceId("block-" + height)),
                DepositAddress.from("0xabcdef0000000000000000000000000000000000", network),
                new AssetSymbol("USDT"), new DepositAtomicAmount(BigInteger.TEN), new ObservationFinality(6, true));
    }

    private static DepositObservationFinalityState state(DepositChainObservation observation, DepositObservationLifecycle lifecycle) {
        return new DepositObservationFinalityState(observation.identity(), observation.block(), 6, lifecycle);
    }
}
