package com.lumix.marketdata.book;

import com.lumix.marketdata.contract.AtomicQuantity;
import com.lumix.marketdata.contract.BookDeltaPayload;
import com.lumix.marketdata.contract.BookLevel;
import com.lumix.marketdata.contract.BookSnapshotPayload;
import com.lumix.marketdata.contract.DecimalPrice;
import com.lumix.marketdata.contract.MarketDataChannel;
import com.lumix.marketdata.contract.MarketDataContractViolation;
import com.lumix.marketdata.contract.MarketDataEventType;
import com.lumix.marketdata.contract.NormalizedMarketDataEvent;
import com.lumix.marketdata.policy.FeedHealth;
import com.lumix.marketdata.policy.MarketDataAdmissionDecision;
import com.lumix.marketdata.policy.MarketDataAdmissionResult;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

/**
 * P21-T04 的 deterministic、無狀態 order-book reducer。
 *
 * <p>呼叫端必須依同一 stream 序列化前一 projection 與 T03 admission result。reducer 不保存 map、
 * 不讀 clock，也不連接 sandbox order book；因此它只能產生唯讀市場資料 projection，不能作為 matching book。</p>
 */
public final class ReadOnlyOrderBookProjectionReducer {

    /**
     * 依已驗證的 admission result 套用單筆 book event。任何 admission 或 stream 不相容都 fail closed。
     */
    public OrderBookProjectionResult reduce(
            Optional<ReadOnlyOrderBookProjection> previousProjection,
            NormalizedMarketDataEvent event,
            MarketDataAdmissionResult admission
    ) {
        previousProjection = Objects.requireNonNull(previousProjection, "previousProjection must not be null");
        event = Objects.requireNonNull(event, "event must not be null");
        admission = Objects.requireNonNull(admission, "admission must not be null");

        ReadOnlyOrderBookProjection base = previousProjection.isPresent()
                ? previousProjection.get()
                : ReadOnlyOrderBookProjection.unavailable(event.streamKey(), OrderBookStatus.UNAVAILABLE);
        if (!base.streamKey().equals(event.streamKey())) {
            return rejected(base, OrderBookProjectionReason.STREAM_KEY_MISMATCH);
        }
        if (event.channel() != MarketDataChannel.BOOK) {
            return rejected(base, OrderBookProjectionReason.NON_BOOK_STREAM);
        }
        if (event.eventType() != MarketDataEventType.BOOK_SNAPSHOT && event.eventType() != MarketDataEventType.BOOK_DELTA) {
            return rejected(base, OrderBookProjectionReason.NON_BOOK_EVENT);
        }
        if (!admission.nextCursor().streamKey().equals(event.streamKey())
                || (admission.shouldApplyEvent() && !admissionCursorMatchesEvent(admission, event))
                || (admission.decision() == MarketDataAdmissionDecision.DUPLICATE_IGNORED
                && !admissionCursorMatchesEvent(admission, event))) {
            return rejected(base, OrderBookProjectionReason.ADMISSION_EVENT_MISMATCH);
        }
        if (!admission.shouldApplyEvent()) {
            if (admission.decision() == MarketDataAdmissionDecision.DUPLICATE_IGNORED) {
                return new OrderBookProjectionResult(
                        OrderBookProjectionDecision.DUPLICATE_IGNORED,
                        OrderBookProjectionReason.DUPLICATE_ADMISSION,
                        base.withStatus(statusFor(admission.nextCursor().health()))
                );
            }
            return rejected(base.withStatus(statusForRejectedAdmission(admission.nextCursor().health())), OrderBookProjectionReason.ADMISSION_NOT_ACCEPTED);
        }
        if (base.lastAppliedIdentity().filter(event.identity()::equals).isPresent()) {
            return rejected(base, OrderBookProjectionReason.PROJECTION_IDENTITY_ALREADY_APPLIED);
        }

        return switch (event.eventType()) {
            case BOOK_SNAPSHOT -> applySnapshot(base, event, admission);
            case BOOK_DELTA -> applyDelta(base, event, admission);
            default -> throw new IllegalStateException("non-book event was rejected before reducer dispatch");
        };
    }

    private OrderBookProjectionResult applySnapshot(
            ReadOnlyOrderBookProjection base,
            NormalizedMarketDataEvent event,
            MarketDataAdmissionResult admission
    ) {
        BookSnapshotPayload payload = (BookSnapshotPayload) event.payload();
        try {
            List<BookLevel> bids = aggregateSnapshotLevels(payload.bids(), event, Comparator.reverseOrder());
            List<BookLevel> asks = aggregateSnapshotLevels(payload.asks(), event, Comparator.naturalOrder());
            if (isCrossed(bids, asks)) {
                return rejected(base.withStatus(OrderBookStatus.DEGRADED), OrderBookProjectionReason.CROSSED_BOOK_REJECTED);
            }
            if (exceedsProjectionLevelLimit(bids, asks)) {
                return rejected(base.withStatus(OrderBookStatus.DEGRADED), OrderBookProjectionReason.LEVEL_LIMIT_EXCEEDED);
            }
            return applied(
                    OrderBookProjectionDecision.SNAPSHOT_APPLIED,
                    OrderBookProjectionReason.SNAPSHOT_BASELINE_ACCEPTED,
                    event,
                    admission,
                    bids,
                    asks
            );
        } catch (ArithmeticException | MarketDataContractViolation exception) {
            return rejected(base.withStatus(OrderBookStatus.DEGRADED), OrderBookProjectionReason.QUANTITY_OVERFLOW);
        }
    }

    private OrderBookProjectionResult applyDelta(
            ReadOnlyOrderBookProjection base,
            NormalizedMarketDataEvent event,
            MarketDataAdmissionResult admission
    ) {
        if (base.asOfSequence().isEmpty() || (base.status() != OrderBookStatus.HEALTHY && base.status() != OrderBookStatus.STALE)) {
            return rejected(base.withStatus(OrderBookStatus.RESYNC_REQUIRED), OrderBookProjectionReason.SNAPSHOT_REQUIRED);
        }
        long expectedSequence = base.asOfSequence().orElseThrow().value() + 1;
        if (event.sequence().value() != expectedSequence) {
            // T03 的 cursor 可能來自較新的平行／錯誤 baseline；projection 不能跨過自身 as-of sequence 套用它。
            return rejected(base.withStatus(OrderBookStatus.RESYNC_REQUIRED), OrderBookProjectionReason.DELTA_SEQUENCE_NOT_CONTINUOUS);
        }
        if (admission.nextCursor().requireLastAcceptedEvent().sequence().value() != expectedSequence) {
            return rejected(base.withStatus(OrderBookStatus.RESYNC_REQUIRED), OrderBookProjectionReason.ADMISSION_CURSOR_BASELINE_MISMATCH);
        }
        BookDeltaPayload payload = (BookDeltaPayload) event.payload();
        try {
            List<BookLevel> bids = applyUpdates(base.bids(), payload.bidUpdates(), event, Comparator.reverseOrder());
            List<BookLevel> asks = applyUpdates(base.asks(), payload.askUpdates(), event, Comparator.naturalOrder());
            if (isCrossed(bids, asks)) {
                return rejected(base.withStatus(OrderBookStatus.DEGRADED), OrderBookProjectionReason.CROSSED_BOOK_REJECTED);
            }
            // delta 本身受限不代表套用後仍受限；不可為了保留舊 book 而部分套用超限更新。
            if (exceedsProjectionLevelLimit(bids, asks)) {
                return rejected(base.withStatus(OrderBookStatus.DEGRADED), OrderBookProjectionReason.LEVEL_LIMIT_EXCEEDED);
            }
            return applied(
                    OrderBookProjectionDecision.DELTA_APPLIED,
                    OrderBookProjectionReason.CONTIGUOUS_DELTA_APPLIED,
                    event,
                    admission,
                    bids,
                    asks
            );
        } catch (ArithmeticException | MarketDataContractViolation exception) {
            return rejected(base.withStatus(OrderBookStatus.DEGRADED), OrderBookProjectionReason.QUANTITY_OVERFLOW);
        }
    }

    private static boolean admissionCursorMatchesEvent(MarketDataAdmissionResult admission, NormalizedMarketDataEvent event) {
        return admission.nextCursor().lastAcceptedEvent()
                .map(accepted -> accepted.identity().equals(event.identity()) && accepted.sequence().equals(event.sequence()))
                .orElse(false);
    }

    private static OrderBookProjectionResult applied(
            OrderBookProjectionDecision decision,
            OrderBookProjectionReason reason,
            NormalizedMarketDataEvent event,
            MarketDataAdmissionResult admission,
            List<BookLevel> bids,
            List<BookLevel> asks
    ) {
        ReadOnlyOrderBookProjection projection = new ReadOnlyOrderBookProjection(
                event.streamKey(),
                Optional.of(event.sequence()),
                Optional.of(event.sourceTimestamp()),
                Optional.of(event.identity()),
                bids,
                asks,
                statusFor(admission.nextCursor().health())
        );
        return new OrderBookProjectionResult(decision, reason, projection);
    }

    private static OrderBookProjectionResult rejected(ReadOnlyOrderBookProjection projection, OrderBookProjectionReason reason) {
        return new OrderBookProjectionResult(OrderBookProjectionDecision.REJECTED, reason, projection);
    }

    private static List<BookLevel> aggregateSnapshotLevels(
            List<BookLevel> levels,
            NormalizedMarketDataEvent event,
            Comparator<BigDecimal> comparator
    ) {
        Map<BigDecimal, BigInteger> quantities = new TreeMap<>(comparator);
        for (BookLevel level : levels) {
            quantities.merge(level.price().value(), level.quantity().atoms(), BigInteger::add);
        }
        return levelsFrom(quantities, event);
    }

    private static List<BookLevel> applyUpdates(
            List<BookLevel> existing,
            List<BookLevel> updates,
            NormalizedMarketDataEvent event,
            Comparator<BigDecimal> comparator
    ) {
        Map<BigDecimal, BigInteger> quantities = new TreeMap<>(comparator);
        existing.forEach(level -> quantities.put(level.price().value(), level.quantity().atoms()));
        // delta 的同價格更新依 payload 固定順序覆寫；這是 provider-neutral contract 中唯一可重放的明確語意。
        for (BookLevel update : updates) {
            if (update.quantity().atoms().signum() == 0) {
                quantities.remove(update.price().value());
            } else {
                quantities.put(update.price().value(), update.quantity().atoms());
            }
        }
        return levelsFrom(quantities, event);
    }

    private static List<BookLevel> levelsFrom(Map<BigDecimal, BigInteger> quantities, NormalizedMarketDataEvent event) {
        List<BookLevel> levels = new ArrayList<>(quantities.size());
        for (Map.Entry<BigDecimal, BigInteger> entry : quantities.entrySet()) {
            AtomicQuantity quantity = AtomicQuantity.fromWire(entry.getValue().toString(), event.precision());
            if (quantity.atoms().signum() > 0) {
                levels.add(new BookLevel(new DecimalPrice(entry.getKey()), quantity));
            }
        }
        return List.copyOf(levels);
    }

    private static boolean isCrossed(List<BookLevel> bids, List<BookLevel> asks) {
        return !bids.isEmpty() && !asks.isEmpty() && bids.getFirst().price().value().compareTo(asks.getFirst().price().value()) >= 0;
    }

    private static boolean exceedsProjectionLevelLimit(List<BookLevel> bids, List<BookLevel> asks) {
        return bids.size() > com.lumix.marketdata.contract.OrderBookLevelLimits.MAX_LEVELS_PER_SIDE
                || asks.size() > com.lumix.marketdata.contract.OrderBookLevelLimits.MAX_LEVELS_PER_SIDE;
    }

    private static OrderBookStatus statusFor(FeedHealth health) {
        return switch (health) {
            case HEALTHY -> OrderBookStatus.HEALTHY;
            case STALE -> OrderBookStatus.STALE;
            case GAP_DETECTED -> OrderBookStatus.GAP_DETECTED;
            case RESYNC_REQUIRED -> OrderBookStatus.RESYNC_REQUIRED;
            case DEGRADED, STOPPED -> OrderBookStatus.DEGRADED;
        };
    }

    private static OrderBookStatus statusForRejectedAdmission(FeedHealth health) {
        // duplicate 已在上方獨立處理；其餘拒絕即使 T03 cursor 仍為 HEALTHY，也代表本 projection 不能再聲稱連續。
        return health == FeedHealth.HEALTHY || health == FeedHealth.STALE
                ? OrderBookStatus.RESYNC_REQUIRED
                : statusFor(health);
    }
}
