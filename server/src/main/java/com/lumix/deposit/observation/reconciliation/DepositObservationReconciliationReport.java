package com.lumix.deposit.observation.reconciliation;

import com.lumix.deposit.address.DepositNetwork;
import java.util.Objects;
import java.util.Optional;

/**
 * 可持久化為 evidence 的唯讀 reconciliation 輸出。
 */
public record DepositObservationReconciliationReport(
        DepositNetwork network,
        ObservationReconciliationStatus status,
        ObservationReconciliationMetrics metrics,
        String evidenceDigest,
        Optional<P23DepositObservationHandoff> p23Handoff
) {

    public DepositObservationReconciliationReport {
        network = Objects.requireNonNull(network, "network must not be null");
        status = Objects.requireNonNull(status, "status must not be null");
        metrics = Objects.requireNonNull(metrics, "metrics must not be null");
        evidenceDigest = Objects.requireNonNull(evidenceDigest, "evidenceDigest must not be null");
        p23Handoff = Objects.requireNonNull(p23Handoff, "p23Handoff must not be null");
        if (status != ObservationReconciliationStatus.RECONCILED && p23Handoff.isPresent()) {
            throw new IllegalArgumentException("blocked reconciliation must not expose P23 handoff candidates");
        }
        if (p23Handoff.isPresent()) {
            P23DepositObservationHandoff handoff = p23Handoff.orElseThrow();
            if (!network.equals(handoff.network()) || !evidenceDigest.equals(handoff.evidenceDigest())) {
                throw new IllegalArgumentException("handoff must retain report network and evidence digest");
            }
        }
    }
}
