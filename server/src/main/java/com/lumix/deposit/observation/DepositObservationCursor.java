package com.lumix.deposit.observation;

import com.lumix.deposit.address.DepositNetwork;
import java.util.Comparator;
import java.util.Objects;

/**
 * 同一 network 內可穩定排序的觀測游標。
 *
 * <p>hash 也列入排序，確保相同高度的不同區塊不會被本機時間或輸入順序影響；reorg 是否可接受由 P22-T03 決定。</p>
 */
public record DepositObservationCursor(
        DepositNetwork network,
        ChainBlockReference block,
        ChainReferenceId transactionId,
        ChainEventIndex eventIndex
) implements Comparable<DepositObservationCursor> {

    private static final Comparator<DepositObservationCursor> CANONICAL_ORDER = Comparator
            .comparingLong((DepositObservationCursor value) -> value.block().height())
            .thenComparing(value -> value.block().hash().value())
            .thenComparing(value -> value.transactionId().value())
            .thenComparingLong(value -> value.eventIndex().value());

    public DepositObservationCursor {
        network = Objects.requireNonNull(network, "network must not be null");
        block = Objects.requireNonNull(block, "block must not be null");
        transactionId = Objects.requireNonNull(transactionId, "transactionId must not be null");
        eventIndex = Objects.requireNonNull(eventIndex, "eventIndex must not be null");
    }

    @Override
    public int compareTo(DepositObservationCursor other) {
        other = Objects.requireNonNull(other, "other must not be null");
        if (!network.equals(other.network)) {
            throw new IllegalArgumentException("cursors from different networks are not comparable");
        }
        return CANONICAL_ORDER.compare(this, other);
    }
}
