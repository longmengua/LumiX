package com.lumix.trading.core.spot.matching;

import com.lumix.trading.core.spot.orderbook.InMemorySpotSandboxOrderBook;
import java.time.Instant;
import java.util.Objects;

/**
 * Spot sandbox matching 的 runtime boundary。
 *
 * 這個 boundary 只允許 sandbox in-memory matching，不代表正式 matching engine、settlement 或 ledger posting 已完成。
 */
public final class SpotSandboxMatchingRuntimeBoundary {

    private final InMemorySpotSandboxMatcher matcher;

    /**
     * 建立 sandbox matching runtime boundary。
     *
     * 這裡只接 in-memory order book，不接任何 DB client 或正式 runtime service。
     */
    public SpotSandboxMatchingRuntimeBoundary(InMemorySpotSandboxOrderBook orderBook) {
        this.matcher = new InMemorySpotSandboxMatcher(
                Objects.requireNonNull(orderBook, "orderBook must not be null"),
                new SpotSandboxTradeFillBoundary()
        );
    }

    /**
     * 執行 sandbox matching。
     *
     * 這個方法只回傳 sandbox match result，不代表 settlement completed、ledger posted 或 balance updated。
     */
    public SpotSandboxMatchResult match(String marketSymbol, Instant matchedAt) {
        return matcher.match(marketSymbol, matchedAt);
    }
}
