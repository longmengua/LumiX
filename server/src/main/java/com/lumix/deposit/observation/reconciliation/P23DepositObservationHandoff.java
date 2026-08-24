package com.lumix.deposit.observation.reconciliation;

import com.lumix.deposit.address.DepositNetwork;
import java.util.List;
import java.util.Objects;

/**
 * P22 對 P23 的 immutable、唯讀證據交接封套。
 */
public record P23DepositObservationHandoff(
        DepositNetwork network,
        String evidenceDigest,
        List<P23DepositHandoffCandidate> candidates
) {

    public P23DepositObservationHandoff {
        network = Objects.requireNonNull(network, "network must not be null");
        evidenceDigest = requireDigest(evidenceDigest);
        candidates = List.copyOf(Objects.requireNonNull(candidates, "candidates must not be null"));
        for (P23DepositHandoffCandidate candidate : candidates) {
            if (!network.equals(candidate.observation().network())) {
                throw new IllegalArgumentException("handoff candidates must belong to the handoff network");
            }
        }
    }

    private static String requireDigest(String value) {
        value = Objects.requireNonNull(value, "evidenceDigest must not be null");
        if (!value.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("evidence digest must be a lowercase SHA-256 hex value");
        }
        return value;
    }
}
