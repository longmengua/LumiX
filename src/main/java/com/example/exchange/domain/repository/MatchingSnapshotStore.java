/*
 * 檔案用途：Repository contract，定義 matching engine snapshot 持久化能力。
 */
package com.example.exchange.domain.repository;

import com.example.exchange.domain.model.dto.MatchingEngineSnapshot;

import java.util.Optional;

/**
 * Per-symbol matching snapshot store。
 *
 * <p>Snapshot 用來縮短 replay recovery 距離；command/event log 仍是權威來源。</p>
 */
public interface MatchingSnapshotStore {

    void save(MatchingEngineSnapshot snapshot);

    Optional<MatchingEngineSnapshot> latest(String symbolCode);
}
