package com.lumix.deposit.observation.finality;

import com.lumix.deposit.address.DepositNetwork;
import com.lumix.deposit.observation.DepositObservationCursor;
import java.time.Instant;
import java.util.Objects;

/**
 * provider 已驗證的連續掃描 checkpoint；不是 RPC client 或持久化實作。
 */
public record ObservationFeedCheckpoint(
        DepositNetwork network,
        DepositObservationCursor cursor,
        Instant observedAt
) {

    public ObservationFeedCheckpoint {
        network = Objects.requireNonNull(network, "network must not be null");
        cursor = Objects.requireNonNull(cursor, "cursor must not be null");
        observedAt = Objects.requireNonNull(observedAt, "observedAt must not be null");
        if (!network.equals(cursor.network())) {
            throw new IllegalArgumentException("checkpoint cursor network must match checkpoint network");
        }
    }
}
