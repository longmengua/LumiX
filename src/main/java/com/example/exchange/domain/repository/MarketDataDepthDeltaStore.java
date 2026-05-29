/*
 * 檔案用途：Repository 介面，定義 depth delta 持久化與 reconnect backfill 查詢契約。
 */
package com.example.exchange.domain.repository;

import com.example.exchange.domain.model.dto.DepthDelta;

import java.time.Instant;
import java.util.List;

public interface MarketDataDepthDeltaStore {

    void append(DepthDelta delta);

    List<DepthDelta> findAfter(String symbol, long afterVersion, int limit);

    default long purgeBefore(Instant cutoff) {
        return 0L;
    }
}
