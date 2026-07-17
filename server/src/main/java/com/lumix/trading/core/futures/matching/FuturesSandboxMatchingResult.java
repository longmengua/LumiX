package com.lumix.trading.core.futures.matching;

import java.util.Objects;
import java.util.Optional;

/**
 * Futures sandbox matching reuse gate 的 immutable 結果。
 *
 * 此結果只保存候選評估結論；不會寫入 order book、也不會把 accepted order 轉為 filled 或 position。
 */
public record FuturesSandboxMatchingResult(
        FuturesSandboxMatchingDecision decision,
        FuturesSandboxMatchingReason reason,
        Optional<FuturesSandboxMatchCandidate> candidate
) {

    public static FuturesSandboxMatchingResult matchEligible(FuturesSandboxMatchCandidate candidate) {
        return new FuturesSandboxMatchingResult(
                FuturesSandboxMatchingDecision.MATCH_ELIGIBLE,
                FuturesSandboxMatchingReason.CROSSED_LIMIT_PRICE,
                Optional.of(candidate)
        );
    }

    public static FuturesSandboxMatchingResult noCross() {
        return new FuturesSandboxMatchingResult(
                FuturesSandboxMatchingDecision.NO_CROSS,
                FuturesSandboxMatchingReason.NO_CROSSED_ORDERS,
                Optional.empty()
        );
    }

    public static FuturesSandboxMatchingResult rejected(FuturesSandboxMatchingReason reason) {
        return new FuturesSandboxMatchingResult(FuturesSandboxMatchingDecision.REJECTED, reason, Optional.empty());
    }

    public FuturesSandboxMatchingResult {
        Objects.requireNonNull(decision, "decision must not be null");
        Objects.requireNonNull(reason, "reason must not be null");
        Objects.requireNonNull(candidate, "candidate must not be null");

        if (decision == FuturesSandboxMatchingDecision.MATCH_ELIGIBLE
                && (reason != FuturesSandboxMatchingReason.CROSSED_LIMIT_PRICE || candidate.isEmpty())) {
            throw new IllegalArgumentException("MATCH_ELIGIBLE result requires CROSSED_LIMIT_PRICE and candidate");
        }
        if (decision != FuturesSandboxMatchingDecision.MATCH_ELIGIBLE && candidate.isPresent()) {
            throw new IllegalArgumentException("non-eligible result must not include candidate");
        }
    }
}
