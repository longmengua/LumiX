/*
 * 檔案用途：JPA adapter，實作 durable matching snapshot store。
 */
package com.example.exchange.domain.repository.jpa;

import com.example.exchange.domain.model.dto.MatchingEngineSnapshot;
import com.example.exchange.domain.model.entity.MatchingEngineSnapshotRecord;
import com.example.exchange.domain.repository.MatchingSnapshotStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JpaMatchingSnapshotStore implements MatchingSnapshotStore {

    private final MatchingEngineSnapshotRecordJpaRepository repository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void save(MatchingEngineSnapshot snapshot) {
        MatchingEngineSnapshotRecord record = new MatchingEngineSnapshotRecord();
        record.setSymbolCode(normalize(snapshot.symbolCode()));
        record.setMatchSequence(snapshot.matchSequence());
        record.setCommandOffset(snapshot.commandOffset());
        record.setEventOffset(snapshot.eventOffset());
        record.setSnapshotPayload(writeSnapshot(snapshot));
        record.setCreatedAt(snapshot.createdAt());
        repository.save(record);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MatchingEngineSnapshot> latest(String symbolCode) {
        return repository.findFirstBySymbolCodeOrderByCommandOffsetDescEventOffsetDescCreatedAtDesc(normalize(symbolCode))
                .map(record -> readSnapshot(record.getSnapshotPayload()));
    }

    private String writeSnapshot(MatchingEngineSnapshot snapshot) {
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (Exception e) {
            throw new IllegalStateException("serialize matching snapshot failed", e);
        }
    }

    private MatchingEngineSnapshot readSnapshot(String json) {
        try {
            return objectMapper.readValue(json, MatchingEngineSnapshot.class);
        } catch (Exception e) {
            throw new IllegalStateException("deserialize matching snapshot failed", e);
        }
    }

    private static String normalize(String symbolCode) {
        return symbolCode == null ? "" : symbolCode.trim().toUpperCase();
    }
}
