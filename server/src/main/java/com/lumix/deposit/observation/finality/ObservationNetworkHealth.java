package com.lumix.deposit.observation.finality;

import com.lumix.deposit.address.DepositNetwork;
import com.lumix.deposit.observation.DepositObservationCursor;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * network-isolated health snapshot；每個 network 必須分開保存與評估，不能相互解除 halt。
 */
public record ObservationNetworkHealth(
        DepositNetwork network,
        ObservationNetworkHealthState state,
        Optional<DepositObservationCursor> lastVerifiedCursor,
        Instant lastProviderSignalAt
) {

    public ObservationNetworkHealth {
        network = Objects.requireNonNull(network, "network must not be null");
        state = Objects.requireNonNull(state, "state must not be null");
        lastVerifiedCursor = Objects.requireNonNull(lastVerifiedCursor, "lastVerifiedCursor must not be null");
        lastProviderSignalAt = Objects.requireNonNull(lastProviderSignalAt, "lastProviderSignalAt must not be null");
        if (lastVerifiedCursor.isPresent()) {
            DepositObservationCursor cursor = lastVerifiedCursor.orElseThrow();
            if (!network.equals(cursor.network())) {
                throw new IllegalArgumentException("health cursor network must match health network");
            }
        }
    }

    public static ObservationNetworkHealth healthy(DepositNetwork network, Instant observedAt) {
        return new ObservationNetworkHealth(network, ObservationNetworkHealthState.HEALTHY, Optional.empty(), observedAt);
    }
}
