package com.lumix.marketdata.book;

import com.lumix.marketdata.contract.BookLevel;
import com.lumix.marketdata.contract.MarketDataEventIdentity;
import com.lumix.marketdata.contract.OrderBookLevelLimits;
import com.lumix.marketdata.contract.Sequence;
import com.lumix.marketdata.contract.StreamKey;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 特定 book stream 的 immutable、唯讀價位快照。
 *
 * <p>沒有 baseline 時 as-of 欄位必須為空；這可避免 rejected delta 被誤記為已套用 sequence。
 * record constructor 也驗證排序、非零數量、crossed book 與固定上限，避免公開建構子繞過 reducer 的 fail-closed 規則。</p>
 */
public record ReadOnlyOrderBookProjection(
        StreamKey streamKey,
        Optional<Sequence> asOfSequence,
        Optional<Instant> asOfSourceTimestamp,
        Optional<MarketDataEventIdentity> lastAppliedIdentity,
        List<BookLevel> bids,
        List<BookLevel> asks,
        OrderBookStatus status
) {

    public ReadOnlyOrderBookProjection {
        streamKey = Objects.requireNonNull(streamKey, "streamKey must not be null");
        asOfSequence = Objects.requireNonNull(asOfSequence, "asOfSequence must not be null");
        asOfSourceTimestamp = Objects.requireNonNull(asOfSourceTimestamp, "asOfSourceTimestamp must not be null");
        lastAppliedIdentity = Objects.requireNonNull(lastAppliedIdentity, "lastAppliedIdentity must not be null");
        bids = List.copyOf(Objects.requireNonNull(bids, "bids must not be null"));
        asks = List.copyOf(Objects.requireNonNull(asks, "asks must not be null"));
        status = Objects.requireNonNull(status, "status must not be null");
        if (asOfSequence.isPresent() != asOfSourceTimestamp.isPresent()
                || asOfSequence.isPresent() != lastAppliedIdentity.isPresent()) {
            throw new IllegalArgumentException("projection as-of metadata must be present together");
        }
        if ((status == OrderBookStatus.HEALTHY || status == OrderBookStatus.STALE) && asOfSequence.isEmpty()) {
            throw new IllegalArgumentException("healthy or stale projection requires complete as-of baseline metadata");
        }
        OrderBookLevelLimits.requireProjectionSideWithinLimit(bids, "bids");
        OrderBookLevelLimits.requireProjectionSideWithinLimit(asks, "asks");
        validateStrictLevels(bids, true);
        validateStrictLevels(asks, false);
        if (status == OrderBookStatus.HEALTHY && isCrossed(bids, asks)) {
            throw new IllegalArgumentException("healthy projection must not be crossed");
        }
    }

    /**
     * 建立未有任何可套用 snapshot 的 projection。空 book 不等同健康 book，必須由 status 明確區分。
     */
    public static ReadOnlyOrderBookProjection unavailable(StreamKey streamKey, OrderBookStatus status) {
        return new ReadOnlyOrderBookProjection(
                streamKey, Optional.empty(), Optional.empty(), Optional.empty(), List.of(), List.of(), status
        );
    }

    /**
     * 只改變可用狀態；非 accepted event 不可改寫 levels、as-of sequence 或 identity。
     */
    public ReadOnlyOrderBookProjection withStatus(OrderBookStatus nextStatus) {
        return new ReadOnlyOrderBookProjection(
                streamKey, asOfSequence, asOfSourceTimestamp, lastAppliedIdentity, bids, asks, nextStatus
        );
    }

    /**
     * 空 book 是資料內容，不是健康狀態；consumer 必須同時檢查此值與 status，不能以空列表推論 feed 壞掉或正常。
     */
    public boolean isEmpty() {
        return bids.isEmpty() && asks.isEmpty();
    }

    private static void validateStrictLevels(List<BookLevel> levels, boolean bids) {
        for (int index = 0; index < levels.size(); index++) {
            BookLevel current = Objects.requireNonNull(levels.get(index), "book level must not be null");
            if (!current.quantity().isPositive()) {
                throw new IllegalArgumentException("projection book level quantity must be positive");
            }
            if (index > 0) {
                int comparison = levels.get(index - 1).price().value().compareTo(current.price().value());
                // bids 必須由高到低、asks 必須由低到高；嚴格比較同時拒絕重複價位。
                if ((bids && comparison <= 0) || (!bids && comparison >= 0)) {
                    throw new IllegalArgumentException("projection book levels must use strict side ordering");
                }
            }
        }
    }

    private static boolean isCrossed(List<BookLevel> bids, List<BookLevel> asks) {
        return !bids.isEmpty() && !asks.isEmpty()
                && bids.getFirst().price().value().compareTo(asks.getFirst().price().value()) >= 0;
    }
}
