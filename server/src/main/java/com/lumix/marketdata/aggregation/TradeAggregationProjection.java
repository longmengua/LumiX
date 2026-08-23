package com.lumix.marketdata.aggregation;

import com.lumix.marketdata.contract.MarketDataEventIdentity;
import com.lumix.marketdata.contract.Sequence;
import com.lumix.marketdata.contract.StreamKey;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * 單一 normalized trade stream 的 immutable aggregation state。
 *
 * <p>tickerWindowTrades 是維持 24h rolling window 所需的最小資料，不是 trade history storage。它有固定上限，
 * 且 package 沒有任何持久化或 shared map；後續 durable recovery 必須由 P21-T06 另行設計。</p>
 */
public record TradeAggregationProjection(
        StreamKey streamKey,
        Optional<Sequence> asOfSequence,
        Optional<Instant> asOfSourceTimestamp,
        Optional<MarketDataEventIdentity> lastAppliedIdentity,
        List<NormalizedTradeObservation> tickerWindowTrades,
        Optional<TickerView> ticker,
        Map<CandleInterval, CandleView> candles,
        AggregationStatus status
) {

    public TradeAggregationProjection {
        streamKey = Objects.requireNonNull(streamKey, "streamKey must not be null");
        asOfSequence = Objects.requireNonNull(asOfSequence, "asOfSequence must not be null");
        asOfSourceTimestamp = Objects.requireNonNull(asOfSourceTimestamp, "asOfSourceTimestamp must not be null");
        lastAppliedIdentity = Objects.requireNonNull(lastAppliedIdentity, "lastAppliedIdentity must not be null");
        tickerWindowTrades = List.copyOf(Objects.requireNonNull(tickerWindowTrades, "tickerWindowTrades must not be null"));
        ticker = Objects.requireNonNull(ticker, "ticker must not be null");
        candles = Map.copyOf(Objects.requireNonNull(candles, "candles must not be null"));
        status = Objects.requireNonNull(status, "status must not be null");
        if (asOfSequence.isPresent() != asOfSourceTimestamp.isPresent()
                || asOfSequence.isPresent() != lastAppliedIdentity.isPresent()) {
            throw new IllegalArgumentException("aggregation as-of metadata must be present together");
        }
        if (asOfSequence.isEmpty() && (!tickerWindowTrades.isEmpty() || ticker.isPresent() || !candles.isEmpty())) {
            throw new IllegalArgumentException("aggregation without baseline must not expose derived views");
        }
        if (ticker.isPresent() != !tickerWindowTrades.isEmpty()) {
            throw new IllegalArgumentException("ticker must be present exactly when its source window has trades");
        }
    }

    /**
     * 建立尚未收到可套用 trade 的投影；空 projection 不會虛構 price、volume 或 candle。
     */
    public static TradeAggregationProjection unavailable(StreamKey streamKey) {
        return new TradeAggregationProjection(
                streamKey, Optional.empty(), Optional.empty(), Optional.empty(), List.of(), Optional.empty(), Map.of(),
                AggregationStatus.UNAVAILABLE
        );
    }

    /**
     * 非套用 transition 只可調整健康狀態，絕不能改寫已發布的 ticker/candle/as-of metadata。
     */
    public TradeAggregationProjection withStatus(AggregationStatus nextStatus) {
        return new TradeAggregationProjection(
                streamKey, asOfSequence, asOfSourceTimestamp, lastAppliedIdentity, tickerWindowTrades, ticker, candles, nextStatus
        );
    }
}
