package com.lumix.marketdata.replay;

import com.lumix.marketdata.aggregation.TradeAggregationProjection;
import com.lumix.marketdata.book.ReadOnlyOrderBookProjection;
import com.lumix.marketdata.contract.StreamKey;
import com.lumix.marketdata.policy.MarketDataStreamCursor;
import java.util.Map;
import java.util.Objects;

/**
 * replay 的 immutable initial/final state；map 不代表 storage，只保存單次 pure coordinator transition 所需資料。
 */
public record MarketDataReplayState(
        Map<StreamKey, MarketDataStreamCursor> cursors,
        Map<StreamKey, ReadOnlyOrderBookProjection> books,
        Map<StreamKey, TradeAggregationProjection> aggregations,
        Map<StreamKey, ResyncRequest> pendingResyncRequests
) {

    public static final int MAX_STREAMS = 256;

    public MarketDataReplayState {
        cursors = Map.copyOf(Objects.requireNonNull(cursors, "cursors must not be null"));
        books = Map.copyOf(Objects.requireNonNull(books, "books must not be null"));
        aggregations = Map.copyOf(Objects.requireNonNull(aggregations, "aggregations must not be null"));
        pendingResyncRequests = Map.copyOf(Objects.requireNonNull(pendingResyncRequests, "pendingResyncRequests must not be null"));
        if (cursors.size() > MAX_STREAMS || books.size() > MAX_STREAMS || aggregations.size() > MAX_STREAMS) {
            throw new IllegalArgumentException("replay state exceeds fixed stream limit");
        }
    }

    /** 建立沒有 cursor、projection 或 recovery request 的 deterministic 起點。 */
    public static MarketDataReplayState empty() {
        return new MarketDataReplayState(Map.of(), Map.of(), Map.of(), Map.of());
    }
}
