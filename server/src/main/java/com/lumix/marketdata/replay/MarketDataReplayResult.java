package com.lumix.marketdata.replay;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** 一次 deterministic replay 的 immutable 輸出與失敗隔離資訊。 */
public record MarketDataReplayResult(
        MarketDataReplayState finalState,
        List<ReplayTransitionTrace> trace,
        ReplayDigest digest,
        Optional<ReplayFailureReason> failureReason
) {

    public MarketDataReplayResult {
        finalState = Objects.requireNonNull(finalState, "finalState must not be null");
        trace = List.copyOf(Objects.requireNonNull(trace, "trace must not be null"));
        digest = Objects.requireNonNull(digest, "digest must not be null");
        failureReason = Objects.requireNonNull(failureReason, "failureReason must not be null");
    }

    /** true 表示整個 batch 在 canonicalize 前即被拒絕，已處理 stream 的 non-healthy trace 不算 batch failure。 */
    public boolean failed() {
        return failureReason.isPresent();
    }
}
