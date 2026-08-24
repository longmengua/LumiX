package com.lumix.deposit.observation.finality;

import java.util.Objects;

/**
 * finality 比對結果及其對 network health 的明確安全訊號。
 */
public record DepositObservationFinalityAssessment(
        DepositObservationFinalityState state,
        ObservationSafetyEvent safetyEvent
) {

    public DepositObservationFinalityAssessment {
        state = Objects.requireNonNull(state, "state must not be null");
        safetyEvent = Objects.requireNonNull(safetyEvent, "safetyEvent must not be null");
    }
}
