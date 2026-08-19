package com.lumix.marketdata.policy;

import com.lumix.marketdata.contract.MarketDataEventIdentity;
import com.lumix.marketdata.contract.Sequence;
import com.lumix.marketdata.contract.StreamKey;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * 已受理事件留下的 immutable stream cursor。
 *
 * <p>cursor 僅保存同一 stream 下一次判定所需的最後受理資料，並不保存 event payload 或任何 projection。
 * 因此呼叫端可自行選擇 in-memory、測試 fixture 或未來獲批准的 durable storage，而不改變 policy 語意。</p>
 */
public record MarketDataStreamCursor(
        StreamKey streamKey,
        Optional<AcceptedMarketDataEvent> lastAcceptedEvent,
        FeedHealth health
) {

    public MarketDataStreamCursor {
        streamKey = Objects.requireNonNull(streamKey, "streamKey must not be null");
        lastAcceptedEvent = Objects.requireNonNull(lastAcceptedEvent, "lastAcceptedEvent must not be null");
        health = Objects.requireNonNull(health, "health must not be null");
        if (lastAcceptedEvent.isEmpty() && health != FeedHealth.RESYNC_REQUIRED) {
            throw new IllegalArgumentException("a cursor without an accepted baseline must require resync");
        }
    }

    /**
     * 建立沒有任何已套用 event 的 resync cursor。拒絕初始 delta 時不能偽造「最後已接受」的 sequence。
     */
    public static MarketDataStreamCursor awaitingResync(StreamKey streamKey) {
        return new MarketDataStreamCursor(streamKey, Optional.empty(), FeedHealth.RESYNC_REQUIRED);
    }

    /**
     * 取得真正最後 accepted 的 event。只有已建立 baseline 的 stream 才可以比較 sequence。
     */
    public AcceptedMarketDataEvent requireLastAcceptedEvent() {
        return lastAcceptedEvent.orElseThrow(
                () -> new IllegalStateException("a stream without an accepted baseline cannot compare sequence")
        );
    }

    /**
     * 只更新 health，不改寫最後受理 identity 或 sequence，確保 duplicate / rejected event 不會被誤當成已套用。
     */
    public MarketDataStreamCursor withHealth(FeedHealth nextHealth) {
        return new MarketDataStreamCursor(streamKey, lastAcceptedEvent, nextHealth);
    }

    /**
     * 完整保存已被 policy 接納的 event metadata；payload 本體刻意不放進 cursor，避免它成為 projection 儲存體。
     */
    public record AcceptedMarketDataEvent(
            Sequence sequence,
            Instant sourceTimestamp,
            Instant receivedTimestamp,
            MarketDataEventIdentity identity
    ) {

        public AcceptedMarketDataEvent {
            sequence = Objects.requireNonNull(sequence, "sequence must not be null");
            sourceTimestamp = Objects.requireNonNull(sourceTimestamp, "sourceTimestamp must not be null");
            receivedTimestamp = Objects.requireNonNull(receivedTimestamp, "receivedTimestamp must not be null");
            identity = Objects.requireNonNull(identity, "identity must not be null");
        }
    }
}
