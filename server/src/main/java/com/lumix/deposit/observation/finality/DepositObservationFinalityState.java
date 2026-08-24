package com.lumix.deposit.observation.finality;

import com.lumix.deposit.observation.ChainBlockReference;
import com.lumix.deposit.observation.DepositObservationIdentity;
import java.util.Objects;

/**
 * 某一 immutable observation identity 的最後已驗證 finality 狀態。
 */
public record DepositObservationFinalityState(
        DepositObservationIdentity identity,
        ChainBlockReference block,
        long confirmationCount,
        DepositObservationLifecycle lifecycle
) {

    public DepositObservationFinalityState {
        identity = Objects.requireNonNull(identity, "identity must not be null");
        block = Objects.requireNonNull(block, "block must not be null");
        if (confirmationCount < 0) {
            throw new IllegalArgumentException("confirmation count must not be negative");
        }
        lifecycle = Objects.requireNonNull(lifecycle, "lifecycle must not be null");
    }
}
