package com.lumix.marketdata.query;

import com.lumix.marketdata.contract.StreamKey;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * 測試與內部 contract 使用的 immutable read-model holder。
 *
 * <p>每次 publish 回傳新 instance，不保留可變 reducer reference 或 subscriber queue；它不是 cache、database
 * 或 production market-data store。</p>
 */
public record InMemoryReadOnlyMarketDataViews(Map<StreamKey, MarketDataViewEnvelope> views) implements ReadOnlyMarketDataQueryPort {

    public static final int MAX_VIEWS = 256;

    public InMemoryReadOnlyMarketDataViews {
        views = Map.copyOf(Objects.requireNonNull(views, "views must not be null"));
        if (views.size() > MAX_VIEWS) {
            throw new IllegalArgumentException("internal read-model exceeds fixed view limit");
        }
    }

    public static InMemoryReadOnlyMarketDataViews empty() {
        return new InMemoryReadOnlyMarketDataViews(Map.of());
    }

    /**
     * 發布較新 immutable envelope；較舊或相同 version 不覆寫，避免 query consumer 看見倒退的 projection。
     */
    public InMemoryReadOnlyMarketDataViews publish(MarketDataViewEnvelope next) {
        next = Objects.requireNonNull(next, "next must not be null");
        MarketDataViewEnvelope existing = views.get(next.streamKey());
        if (existing != null && next.projectionVersion() <= existing.projectionVersion()) {
            return this;
        }
        if (existing == null && views.size() >= MAX_VIEWS) {
            throw new IllegalStateException("internal read-model fixed view limit exceeded");
        }
        Map<StreamKey, MarketDataViewEnvelope> replaced = new HashMap<>(views);
        replaced.put(next.streamKey(), next);
        return new InMemoryReadOnlyMarketDataViews(replaced);
    }

    @Override
    public Optional<MarketDataViewEnvelope> current(StreamKey streamKey) {
        return Optional.ofNullable(views.get(Objects.requireNonNull(streamKey, "streamKey must not be null")));
    }
}
