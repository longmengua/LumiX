package com.lumix.deposit.observation;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * pure observation admission 的結果，讓未來 persistence/replay boundary 能明確保存新增與忽略資料。
 */
public record DepositObservationEvaluation(
        List<DepositChainObservation> accepted,
        List<DepositChainObservation> duplicateIgnored,
        Optional<DepositObservationCursor> nextCursor
) {

    public DepositObservationEvaluation {
        accepted = List.copyOf(Objects.requireNonNull(accepted, "accepted must not be null"));
        duplicateIgnored = List.copyOf(Objects.requireNonNull(duplicateIgnored, "duplicateIgnored must not be null"));
        nextCursor = Objects.requireNonNull(nextCursor, "nextCursor must not be null");
    }
}
