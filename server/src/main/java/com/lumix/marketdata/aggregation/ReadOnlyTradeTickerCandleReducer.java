package com.lumix.marketdata.aggregation;

import com.lumix.marketdata.contract.AtomicQuantity;
import com.lumix.marketdata.contract.DecimalPrice;
import com.lumix.marketdata.contract.MarketDataChannel;
import com.lumix.marketdata.contract.MarketDataEventType;
import com.lumix.marketdata.contract.NormalizedMarketDataEvent;
import com.lumix.marketdata.contract.TradePayload;
import com.lumix.marketdata.policy.FeedHealth;
import com.lumix.marketdata.policy.MarketDataAdmissionDecision;
import com.lumix.marketdata.policy.MarketDataAdmissionResult;
import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * P21-T05 的 deterministic、無狀態 trade/ticker/candle reducer。
 *
 * <p>呼叫端須對同一 trade stream 序列化前一 projection 與 T03 admission result。此 reducer 不讀取 wall clock、
 * 不保存 shared state，也不連接 matching、fill、PnL 或任何資金相關路徑；它只產生可重放的唯讀市場資料視圖。</p>
 */
public final class ReadOnlyTradeTickerCandleReducer {

    /** 24h ticker window 的資料保留上限；超限一律拒絕，不能悄悄截斷而改變成交量。 */
    public static final int MAX_TICKER_WINDOW_TRADES = 1_024;
    private static final Duration TICKER_WINDOW = Duration.ofHours(24);

    /**
     * 依已驗證的 T03 admission result 套用單筆 normalized public trade。任何不連續、非健康或時間衝突皆 fail closed。
     */
    public TradeAggregationResult reduce(
            Optional<TradeAggregationProjection> previousProjection,
            NormalizedMarketDataEvent event,
            MarketDataAdmissionResult admission
    ) {
        previousProjection = Objects.requireNonNull(previousProjection, "previousProjection must not be null");
        event = Objects.requireNonNull(event, "event must not be null");
        admission = Objects.requireNonNull(admission, "admission must not be null");

        TradeAggregationProjection base = previousProjection.isPresent()
                ? previousProjection.get()
                : TradeAggregationProjection.unavailable(event.streamKey());
        if (!base.streamKey().equals(event.streamKey())) {
            return rejected(base, TradeAggregationReason.STREAM_KEY_MISMATCH);
        }
        if (event.channel() != MarketDataChannel.TRADES) {
            return rejected(base, TradeAggregationReason.NON_TRADE_STREAM);
        }
        if (event.eventType() != MarketDataEventType.TRADE) {
            return rejected(base, TradeAggregationReason.NON_TRADE_EVENT);
        }
        if (!admission.nextCursor().streamKey().equals(event.streamKey())
                || (admission.shouldApplyEvent() && !admissionCursorMatchesEvent(admission, event))
                || (admission.decision() == MarketDataAdmissionDecision.DUPLICATE_IGNORED
                && !admissionCursorMatchesEvent(admission, event))) {
            return rejected(base, TradeAggregationReason.ADMISSION_EVENT_MISMATCH);
        }
        if (!admission.shouldApplyEvent()) {
            if (admission.decision() == MarketDataAdmissionDecision.DUPLICATE_IGNORED) {
                return new TradeAggregationResult(
                        TradeAggregationDecision.DUPLICATE_IGNORED,
                        TradeAggregationReason.DUPLICATE_ADMISSION,
                        base.withStatus(statusFor(admission.nextCursor().health()))
                );
            }
            return rejected(base.withStatus(statusForRejectedAdmission(admission.nextCursor().health())), TradeAggregationReason.ADMISSION_NOT_ACCEPTED);
        }
        if (admission.nextCursor().health() != FeedHealth.HEALTHY) {
            // accepted 只表示 sequence 合法；stale data 仍不可更新可被誤解為完整的 ticker/candle。
            return rejected(base.withStatus(statusFor(admission.nextCursor().health())), TradeAggregationReason.FEED_NOT_HEALTHY);
        }
        if (base.lastAppliedIdentity().filter(event.identity()::equals).isPresent()) {
            return rejected(base, TradeAggregationReason.PROJECTION_IDENTITY_ALREADY_APPLIED);
        }
        if (base.asOfSequence().isPresent() && event.sequence().value() != base.asOfSequence().orElseThrow().value() + 1) {
            return rejected(base.withStatus(AggregationStatus.RESYNC_REQUIRED), TradeAggregationReason.SEQUENCE_NOT_CONTINUOUS);
        }
        if (base.asOfSourceTimestamp().isPresent() && event.sourceTimestamp().isBefore(base.asOfSourceTimestamp().orElseThrow())) {
            // source-time window 不允許回填；無持久化 history 時回填會讓既發 candle/ticker 在 consumer 間不一致。
            return rejected(base.withStatus(AggregationStatus.DEGRADED), TradeAggregationReason.LATE_SOURCE_EVENT);
        }

        try {
            TradePayload payload = (TradePayload) event.payload();
            NormalizedTradeObservation observation = new NormalizedTradeObservation(
                    event.identity(), event.sequence(), event.sourceTimestamp(), payload.price(), payload.quantity(),
                    QuoteVolume.forTrade(payload.price(), payload.quantity(), event.precision())
            );
            List<NormalizedTradeObservation> tickerTrades = nextTickerWindow(base.tickerWindowTrades(), observation);
            if (tickerTrades.size() > MAX_TICKER_WINDOW_TRADES) {
                return rejected(base.withStatus(AggregationStatus.DEGRADED), TradeAggregationReason.WINDOW_TRADE_LIMIT_EXCEEDED);
            }
            Map<CandleInterval, CandleView> candles = nextCandles(base.candles(), observation, event);
            TickerView ticker = tickerFrom(tickerTrades, observation, event);
            TradeAggregationProjection projection = new TradeAggregationProjection(
                    event.streamKey(), Optional.of(event.sequence()), Optional.of(event.sourceTimestamp()), Optional.of(event.identity()),
                    tickerTrades, Optional.of(ticker), candles, AggregationStatus.HEALTHY
            );
            return new TradeAggregationResult(TradeAggregationDecision.APPLIED, TradeAggregationReason.TRADE_APPLIED, projection);
        } catch (ArithmeticException exception) {
            return rejected(base.withStatus(AggregationStatus.DEGRADED), TradeAggregationReason.NUMERIC_OVERFLOW);
        }
    }

    private static List<NormalizedTradeObservation> nextTickerWindow(
            List<NormalizedTradeObservation> previousTrades,
            NormalizedTradeObservation incoming
    ) {
        Instant startInclusive = incoming.sourceTimestamp().minus(TICKER_WINDOW);
        List<NormalizedTradeObservation> retained = new ArrayList<>();
        for (NormalizedTradeObservation trade : previousTrades) {
            if (!trade.sourceTimestamp().isBefore(startInclusive)) {
                retained.add(trade);
            }
        }
        retained.add(incoming);
        return List.copyOf(retained);
    }

    private static Map<CandleInterval, CandleView> nextCandles(
            Map<CandleInterval, CandleView> previous,
            NormalizedTradeObservation incoming,
            NormalizedMarketDataEvent event
    ) {
        Map<CandleInterval, CandleView> next = new EnumMap<>(CandleInterval.class);
        next.putAll(previous);
        for (CandleInterval interval : CandleInterval.values()) {
            CandleWindow window = interval.windowFor(incoming.sourceTimestamp());
            CandleView existing = previous.get(interval);
            if (existing == null || window.startInclusive().isAfter(existing.window().startInclusive())) {
                next.put(interval, newCandle(interval, window, incoming));
            } else if (window.startInclusive().equals(existing.window().startInclusive())) {
                next.put(interval, appendCandle(existing, incoming, event));
            } else {
                throw new ArithmeticException("late source-time candle window");
            }
        }
        return Map.copyOf(next);
    }

    private static CandleView newCandle(CandleInterval interval, CandleWindow window, NormalizedTradeObservation trade) {
        if (!window.contains(trade.sourceTimestamp())) {
            throw new IllegalArgumentException("trade must belong to its candle window");
        }
        return new CandleView(
                interval, window, trade.price(), trade.price(), trade.price(), trade.price(), trade.quantity(), trade.quoteVolume(),
                1, trade.sequence(), trade.sourceTimestamp()
        );
    }

    private static CandleView appendCandle(
            CandleView existing,
            NormalizedTradeObservation trade,
            NormalizedMarketDataEvent event
    ) {
        if (!existing.window().contains(trade.sourceTimestamp())) {
            throw new IllegalArgumentException("trade must remain inside existing candle window");
        }
        return new CandleView(
                existing.interval(), existing.window(), existing.open(), maximum(existing.high(), trade.price()), minimum(existing.low(), trade.price()),
                trade.price(), sumQuantity(existing.baseVolume(), trade.quantity(), event),
                existing.quoteVolume().add(trade.quoteVolume(), event.precision()), Math.addExact(existing.tradeCount(), 1),
                trade.sequence(), trade.sourceTimestamp()
        );
    }

    private static TickerView tickerFrom(
            List<NormalizedTradeObservation> trades,
            NormalizedTradeObservation incoming,
            NormalizedMarketDataEvent event
    ) {
        if (trades.isEmpty()) {
            throw new IllegalArgumentException("ticker cannot be derived from an empty window");
        }
        NormalizedTradeObservation first = trades.getFirst();
        DecimalPrice high = first.price();
        DecimalPrice low = first.price();
        AtomicQuantity baseVolume = first.quantity();
        QuoteVolume quoteVolume = first.quoteVolume();
        for (int index = 1; index < trades.size(); index++) {
            NormalizedTradeObservation trade = trades.get(index);
            high = maximum(high, trade.price());
            low = minimum(low, trade.price());
            baseVolume = sumQuantity(baseVolume, trade.quantity(), event);
            quoteVolume = quoteVolume.add(trade.quoteVolume(), event.precision());
        }
        return new TickerView(
                incoming.sourceTimestamp().minus(TICKER_WINDOW), incoming.sourceTimestamp(), first.price(), high, low, incoming.price(),
                baseVolume, quoteVolume, trades.size(), incoming.sequence(), incoming.sourceTimestamp()
        );
    }

    private static AtomicQuantity sumQuantity(AtomicQuantity left, AtomicQuantity right, NormalizedMarketDataEvent event) {
        BigInteger sum = left.atoms().add(right.atoms());
        return AtomicQuantity.fromWire(sum.toString(), event.precision());
    }

    private static DecimalPrice maximum(DecimalPrice left, DecimalPrice right) {
        return left.value().compareTo(right.value()) >= 0 ? left : right;
    }

    private static DecimalPrice minimum(DecimalPrice left, DecimalPrice right) {
        return left.value().compareTo(right.value()) <= 0 ? left : right;
    }

    private static boolean admissionCursorMatchesEvent(MarketDataAdmissionResult admission, NormalizedMarketDataEvent event) {
        return admission.nextCursor().lastAcceptedEvent()
                .map(accepted -> accepted.identity().equals(event.identity()) && accepted.sequence().equals(event.sequence()))
                .orElse(false);
    }

    private static TradeAggregationResult rejected(TradeAggregationProjection projection, TradeAggregationReason reason) {
        return new TradeAggregationResult(TradeAggregationDecision.REJECTED, reason, projection);
    }

    private static AggregationStatus statusFor(FeedHealth health) {
        return switch (health) {
            case HEALTHY -> AggregationStatus.HEALTHY;
            case STALE -> AggregationStatus.STALE;
            case GAP_DETECTED -> AggregationStatus.GAP_DETECTED;
            case RESYNC_REQUIRED -> AggregationStatus.RESYNC_REQUIRED;
            case DEGRADED, STOPPED -> AggregationStatus.DEGRADED;
        };
    }

    private static AggregationStatus statusForRejectedAdmission(FeedHealth health) {
        return health == FeedHealth.HEALTHY || health == FeedHealth.STALE
                ? AggregationStatus.RESYNC_REQUIRED
                : statusFor(health);
    }
}
