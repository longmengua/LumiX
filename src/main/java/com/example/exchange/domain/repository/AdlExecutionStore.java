/*
 * 檔案用途：Repository 介面，定義 ADL forced execution idempotency / audit record 持久化契約。
 */
package com.example.exchange.domain.repository;

import com.example.exchange.domain.model.dto.AdlDeleveragingPlan;
import com.example.exchange.domain.model.dto.AdlExecutionResult;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface AdlExecutionStore {

    int SCHEMA_VERSION = 1;

    Optional<AdlExecutionResult> findCompleted(String commandId);

    default List<AdlExecutionResult> findRecent(int limit) {
        return List.of();
    }

    boolean tryStart(String commandId, AdlDeleveragingPlan plan, Instant startedAt);

    void complete(AdlExecutionResult result);

    void reject(AdlExecutionResult result);
}
