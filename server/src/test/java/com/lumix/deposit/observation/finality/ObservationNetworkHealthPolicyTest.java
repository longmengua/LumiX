package com.lumix.deposit.observation.finality;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.lumix.deposit.address.DepositAddressFormat;
import com.lumix.deposit.address.DepositNetwork;
import com.lumix.deposit.observation.ChainBlockReference;
import com.lumix.deposit.observation.ChainEventIndex;
import com.lumix.deposit.observation.ChainReferenceId;
import com.lumix.deposit.observation.DepositObservationCursor;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ObservationNetworkHealthPolicyTest {

    private final ObservationNetworkHealthPolicy policy = new ObservationNetworkHealthPolicy();
    private final DepositNetwork ethereum = new DepositNetwork("ETH_MAINNET", DepositAddressFormat.EVM_HEX);
    private final DepositNetwork bitcoin = new DepositNetwork("BTC_MAINNET", DepositAddressFormat.BECH32);
    private final Instant start = Instant.parse("2026-08-24T00:00:00Z");

    @Test
    void reorgAndGapHaltOnlyTheAffectedNetworkAndRecoveryIsExplicit() {
        ObservationNetworkHealth ethereumHealth = ObservationNetworkHealth.healthy(ethereum, start);
        ObservationNetworkHealth bitcoinHealth = ObservationNetworkHealth.healthy(bitcoin, start);

        ObservationNetworkHealth haltedForReorg = policy.applySafetyEvent(
                ethereumHealth, ObservationSafetyEvent.REORG_DETECTED, start.plusSeconds(1));
        ObservationNetworkHealth haltedForGap = policy.applyCheckpoint(
                policy.applyCheckpoint(ObservationNetworkHealth.healthy(ethereum, start), checkpoint(ethereum, 10, start.plusSeconds(1)), 1),
                checkpoint(ethereum, 12, start.plusSeconds(2)), 1);
        ObservationNetworkHealth recovered = policy.resume(haltedForReorg,
                new ObservationHealthRecovery(ethereum, cursor(ethereum, 11), start.plusSeconds(3)));

        assertEquals(ObservationNetworkHealthState.HALTED_REORG, haltedForReorg.state());
        assertEquals(ObservationNetworkHealthState.HALTED_CURSOR_GAP, haltedForGap.state());
        assertEquals(ObservationNetworkHealthState.HEALTHY, recovered.state());
        assertEquals(ObservationNetworkHealthState.HEALTHY, bitcoinHealth.state());
        assertThrows(IllegalArgumentException.class, () -> policy.applyCheckpoint(ethereumHealth,
                checkpoint(bitcoin, 1, start.plusSeconds(1)), 1));
    }

    @Test
    void staleSignalHaltsAndNoHaltedStateCanBeSilentlyAdvanced() {
        ObservationNetworkHealth healthy = policy.applyCheckpoint(
                ObservationNetworkHealth.healthy(ethereum, start), checkpoint(ethereum, 10, start.plusSeconds(1)), 1);
        ObservationNetworkHealth stale = policy.assessStaleness(healthy, start.plusSeconds(62), Duration.ofSeconds(60));
        ObservationNetworkHealth attemptedAdvance = policy.applyCheckpoint(
                stale, checkpoint(ethereum, 11, start.plusSeconds(63)), 1);

        assertEquals(ObservationNetworkHealthState.HALTED_STALE, stale.state());
        assertEquals(ObservationNetworkHealthState.HALTED_STALE, attemptedAdvance.state());
        assertEquals(10, attemptedAdvance.lastVerifiedCursor().orElseThrow().block().height());
    }

    private static ObservationFeedCheckpoint checkpoint(DepositNetwork network, long height, Instant observedAt) {
        return new ObservationFeedCheckpoint(network, cursor(network, height), observedAt);
    }

    private static DepositObservationCursor cursor(DepositNetwork network, long height) {
        return new DepositObservationCursor(network, new ChainBlockReference(height, new ChainReferenceId("block-" + height)),
                new ChainReferenceId("tx-" + height), new ChainEventIndex(0));
    }
}
