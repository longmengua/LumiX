package com.lumix.deposit.observation.reconciliation;

import com.lumix.deposit.address.DepositNetwork;
import com.lumix.deposit.observation.DepositChainObservation;
import com.lumix.deposit.observation.DepositObservationIdentity;
import com.lumix.deposit.observation.finality.DepositObservationFinalityState;
import com.lumix.deposit.observation.finality.DepositObservationLifecycle;
import com.lumix.deposit.observation.finality.ObservationNetworkHealth;
import com.lumix.deposit.observation.finality.ObservationNetworkHealthState;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * P22-T04 的 pure evidence reconciliation。
 *
 * <p>輸入順序不會影響輸出 digest；觀測、finality state 與 network health 任一缺漏或衝突都 fail-closed，並清空 handoff。
 * 本類別沒有任何 I/O 或帳務相依。</p>
 */
public final class DepositObservationReconciliationPolicy {

    public DepositObservationReconciliationReport evaluate(
            DepositNetwork network,
            ObservationNetworkHealth health,
            Collection<DepositChainObservation> observations,
            Map<DepositObservationIdentity, DepositObservationFinalityState> finalityEvidence
    ) {
        DepositNetwork requestedNetwork = Objects.requireNonNull(network, "network must not be null");
        ObservationNetworkHealth networkHealth = Objects.requireNonNull(health, "health must not be null");
        List<DepositChainObservation> suppliedObservations = List.copyOf(
                Objects.requireNonNull(observations, "observations must not be null"));
        Map<DepositObservationIdentity, DepositObservationFinalityState> suppliedFinality = Map.copyOf(
                Objects.requireNonNull(finalityEvidence, "finalityEvidence must not be null"));
        requireNetwork(requestedNetwork, networkHealth.network(), "health");

        List<DepositChainObservation> ordered = suppliedObservations.stream()
                .peek(observation -> requireNetwork(requestedNetwork, observation.network(), "observation"))
                .sorted(Comparator.comparing(DepositChainObservation::cursor))
                .toList();
        Map<DepositObservationIdentity, DepositChainObservation> unique = new LinkedHashMap<>();
        boolean conflicting = false;
        for (DepositChainObservation observation : ordered) {
            DepositChainObservation previous = unique.putIfAbsent(observation.identity(), observation);
            if (previous != null && !previous.equals(observation)) {
                conflicting = true;
            }
        }
        for (DepositObservationFinalityState state : suppliedFinality.values()) {
            requireNetwork(requestedNetwork, state.identity().network(), "finality evidence");
        }

        boolean missing = false;
        List<P23DepositHandoffCandidate> candidates = new ArrayList<>();
        for (DepositChainObservation observation : unique.values()) {
            DepositObservationFinalityState state = suppliedFinality.get(observation.identity());
            if (state == null) {
                missing = true;
                continue;
            }
            if (!observation.block().equals(state.block())) {
                conflicting = true;
                continue;
            }
            if (state.lifecycle() == DepositObservationLifecycle.FINALITY_THRESHOLD_MET) {
                candidates.add(new P23DepositHandoffCandidate(observation, state));
            }
        }
        if (!suppliedFinality.keySet().equals(unique.keySet())) {
            missing = true;
        }

        ObservationReconciliationMetrics metrics = metrics(unique.values(), suppliedFinality);
        String digest = digest(requestedNetwork, ordered, suppliedFinality);
        ObservationReconciliationStatus status = status(networkHealth, missing, conflicting);
        Optional<P23DepositObservationHandoff> handoff = status == ObservationReconciliationStatus.RECONCILED
                ? Optional.of(new P23DepositObservationHandoff(requestedNetwork, digest, candidates))
                : Optional.empty();
        return new DepositObservationReconciliationReport(requestedNetwork, status, metrics, digest, handoff);
    }

    private static ObservationReconciliationStatus status(
            ObservationNetworkHealth health, boolean missing, boolean conflicting
    ) {
        if (health.state() != ObservationNetworkHealthState.HEALTHY) {
            return ObservationReconciliationStatus.BLOCKED_NETWORK_HEALTH;
        }
        if (conflicting) {
            return ObservationReconciliationStatus.CONFLICTING_EVIDENCE;
        }
        return missing ? ObservationReconciliationStatus.MISSING_EVIDENCE : ObservationReconciliationStatus.RECONCILED;
    }

    private static ObservationReconciliationMetrics metrics(
            Collection<DepositChainObservation> observations,
            Map<DepositObservationIdentity, DepositObservationFinalityState> finality
    ) {
        int pending = 0;
        int threshold = 0;
        int orphaned = 0;
        int quarantined = 0;
        for (DepositChainObservation observation : observations) {
            DepositObservationFinalityState state = finality.get(observation.identity());
            if (state == null) {
                continue;
            }
            switch (state.lifecycle()) {
                case PENDING_CONFIRMATION -> pending++;
                case FINALITY_THRESHOLD_MET -> threshold++;
                case ORPHANED -> orphaned++;
                case QUARANTINED -> quarantined++;
            }
        }
        return new ObservationReconciliationMetrics(observations.size(), pending, threshold, orphaned, quarantined);
    }

    private static void requireNetwork(DepositNetwork expected, DepositNetwork actual, String valueName) {
        if (!expected.equals(actual)) {
            throw new IllegalArgumentException(valueName + " network does not match requested network");
        }
    }

    private static String digest(
            DepositNetwork network,
            List<DepositChainObservation> observations,
            Map<DepositObservationIdentity, DepositObservationFinalityState> finality
    ) {
        StringBuilder material = new StringBuilder(network.code()).append('\n');
        observations.forEach(observation -> material.append(observation.identity()).append('|')
                .append(observation.block()).append('|').append(observation.amount().atoms()).append('|')
                .append(observation.finality()).append('\n'));
        finality.entrySet().stream().sorted(Map.Entry.comparingByKey(Comparator.comparing(DepositObservationIdentity::toString)))
                .forEach(entry -> material.append(entry.getKey()).append('|').append(entry.getValue()).append('\n'));
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(material.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 must exist in the Java runtime", exception);
        }
    }
}
