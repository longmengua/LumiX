package com.lumix.marketdata.query;

import com.lumix.marketdata.aggregation.TradeAggregationProjection;
import com.lumix.marketdata.book.ReadOnlyOrderBookProjection;
import com.lumix.marketdata.contract.StreamKey;
import java.time.Instant;

/** 所有 internal consumer 都只能經由此 sealed view 讀取 immutable projection，不能取得 reducer state。 */
public sealed interface MarketDataReadOnlyView permits MarketDataReadOnlyView.OrderBook, MarketDataReadOnlyView.TradeAggregation {

    StreamKey streamKey();

    long asOfSequence();

    Instant asOfSourceTimestamp();

    String health();

    /** T04 projection 的 read-only transport form；非健康 status 仍原樣可見。 */
    record OrderBook(ReadOnlyOrderBookProjection projection) implements MarketDataReadOnlyView {
        public OrderBook {
            if (projection == null || projection.asOfSequence().isEmpty() || projection.asOfSourceTimestamp().isEmpty()) {
                throw new IllegalArgumentException("order-book view requires complete immutable projection metadata");
            }
        }

        @Override public StreamKey streamKey() { return projection.streamKey(); }
        @Override public long asOfSequence() { return projection.asOfSequence().orElseThrow().value(); }
        @Override public Instant asOfSourceTimestamp() { return projection.asOfSourceTimestamp().orElseThrow(); }
        @Override public String health() { return projection.status().name(); }
    }

    /** T05 projection 的 read-only transport form；ticker/candle 不存在時也不會自行填入價格。 */
    record TradeAggregation(TradeAggregationProjection projection) implements MarketDataReadOnlyView {
        public TradeAggregation {
            if (projection == null || projection.asOfSequence().isEmpty() || projection.asOfSourceTimestamp().isEmpty()) {
                throw new IllegalArgumentException("trade aggregation view requires complete immutable projection metadata");
            }
        }

        @Override public StreamKey streamKey() { return projection.streamKey(); }
        @Override public long asOfSequence() { return projection.asOfSequence().orElseThrow().value(); }
        @Override public Instant asOfSourceTimestamp() { return projection.asOfSourceTimestamp().orElseThrow(); }
        @Override public String health() { return projection.status().name(); }
    }
}
