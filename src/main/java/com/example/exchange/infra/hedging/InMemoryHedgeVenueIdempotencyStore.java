/*
 * 檔案用途：測試/本地用 hedge venue idempotency store，模擬 durable claim/result 語意。
 */
package com.example.exchange.infra.hedging;

import com.example.exchange.domain.model.dto.HedgeOrderResult;
import com.example.exchange.domain.model.dto.HedgeVenueIdempotencyRecord;
import com.example.exchange.domain.repository.HedgeVenueIdempotencyStore;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

class InMemoryHedgeVenueIdempotencyStore implements HedgeVenueIdempotencyStore {

    private final Map<String, HedgeVenueIdempotencyRecord> records =
            new ConcurrentHashMap<>();

    @Override
    public Optional<HedgeVenueIdempotencyRecord> find(String refId) {
        return Optional.ofNullable(records.get(refId));
    }

    @Override
    public boolean claim(String refId, String fingerprint) {
        HedgeVenueIdempotencyRecord record =
                new HedgeVenueIdempotencyRecord(refId, fingerprint, false, null);
        return records.putIfAbsent(refId, record) == null;
    }

    @Override
    public HedgeVenueIdempotencyRecord complete(String refId, String fingerprint, HedgeOrderResult result) {
        return records.compute(refId, (key, existing) -> {
            if (existing == null || !existing.fingerprint().equals(fingerprint)) {
                return existing;
            }
            return new HedgeVenueIdempotencyRecord(refId, fingerprint, true, result);
        });
    }

    @Override
    public List<HedgeVenueIdempotencyRecord> findUnresolved(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 500));
        return records.values().stream()
                .filter(record -> !record.completed()
                        || (record.result() != null && record.result().retryable()))
                .limit(safeLimit)
                .toList();
    }
}
