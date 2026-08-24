package com.lumix.marketdata.replay;

import com.lumix.marketdata.contract.NormalizedMarketDataEvent;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * 一次 pure replay 的明確輸入。batch 上限保護 fixture/in-memory caller 不會把無界不可信事件送入 coordinator。
 */
public record MarketDataReplayInput(
        MarketDataReplayState initialState,
        List<NormalizedMarketDataEvent> events,
        Instant evaluationTimestamp
) {

    public static final int MAX_EVENTS_PER_BATCH = 4_096;

    public MarketDataReplayInput {
        initialState = Objects.requireNonNull(initialState, "initialState must not be null");
        events = List.copyOf(Objects.requireNonNull(events, "events must not be null"));
        evaluationTimestamp = Objects.requireNonNull(evaluationTimestamp, "evaluationTimestamp must not be null");
        if (events.size() > MAX_EVENTS_PER_BATCH) {
            throw new IllegalArgumentException("replay event batch exceeds fixed limit");
        }
    }
}
