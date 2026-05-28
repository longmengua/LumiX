/*
 * 檔案用途：JPA adapter，實作 durable matching sequencer lease store。
 */
package com.example.exchange.domain.repository.jpa;

import com.example.exchange.domain.model.dto.MatchingSequencerLease;
import com.example.exchange.domain.model.entity.MatchingSequencerLeaseRecord;
import com.example.exchange.domain.repository.MatchingSequencerLeaseStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JpaMatchingSequencerLeaseStore implements MatchingSequencerLeaseStore {

    private final MatchingSequencerLeaseRecordJpaRepository repository;

    @Override
    @Transactional
    public Optional<MatchingSequencerLease> acquire(String symbolCode, String ownerId, Duration ttl, Instant now) {
        String symbol = normalize(symbolCode);
        MatchingSequencerLeaseRecord record = repository.findLocked(symbol)
                .orElseGet(() -> newLease(symbol));
        if (record.getOwnerId() != null
                && !record.getOwnerId().equals(ownerId)
                && record.getExpiresAt().isAfter(now)) {
            return Optional.empty();
        }

        boolean ownerChanged = record.getOwnerId() == null || !record.getOwnerId().equals(ownerId);
        record.setOwnerId(ownerId);
        record.setEpoch(ownerChanged ? record.getEpoch() + 1 : record.getEpoch());
        record.setExpiresAt(now.plus(ttl));
        record.setUpdatedAt(now);
        return Optional.of(toLease(repository.save(record)));
    }

    @Override
    @Transactional
    public Optional<MatchingSequencerLease> renew(
            String symbolCode,
            String ownerId,
            long epoch,
            Duration ttl,
            long commandOffset,
            long eventOffset,
            Instant now
    ) {
        Optional<MatchingSequencerLeaseRecord> locked = repository.findLocked(normalize(symbolCode));
        if (locked.isEmpty()) return Optional.empty();
        MatchingSequencerLeaseRecord record = locked.get();
        if (!record.getOwnerId().equals(ownerId) || record.getEpoch() != epoch || !record.getExpiresAt().isAfter(now)) {
            return Optional.empty();
        }
        record.setCommandOffset(Math.max(0L, commandOffset));
        record.setEventOffset(Math.max(0L, eventOffset));
        record.setExpiresAt(now.plus(ttl));
        record.setUpdatedAt(now);
        return Optional.of(toLease(repository.save(record)));
    }

    @Override
    @Transactional
    public boolean release(String symbolCode, String ownerId, long epoch, Instant now) {
        Optional<MatchingSequencerLeaseRecord> locked = repository.findLocked(normalize(symbolCode));
        if (locked.isEmpty()) return false;
        MatchingSequencerLeaseRecord record = locked.get();
        if (!record.getOwnerId().equals(ownerId) || record.getEpoch() != epoch) return false;
        record.setExpiresAt(now);
        record.setUpdatedAt(now);
        repository.save(record);
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MatchingSequencerLease> current(String symbolCode) {
        return repository.findById(normalize(symbolCode)).map(this::toLease);
    }

    private static MatchingSequencerLeaseRecord newLease(String symbolCode) {
        MatchingSequencerLeaseRecord record = new MatchingSequencerLeaseRecord();
        record.setSymbolCode(symbolCode);
        record.setOwnerId(null);
        record.setEpoch(0L);
        record.setExpiresAt(Instant.EPOCH);
        record.setCommandOffset(0L);
        record.setEventOffset(0L);
        record.setUpdatedAt(Instant.EPOCH);
        return record;
    }

    private MatchingSequencerLease toLease(MatchingSequencerLeaseRecord record) {
        return new MatchingSequencerLease(
                record.getSymbolCode(),
                record.getOwnerId(),
                record.getEpoch(),
                record.getExpiresAt(),
                record.getCommandOffset(),
                record.getEventOffset(),
                record.getUpdatedAt()
        );
    }

    private static String normalize(String symbolCode) {
        return symbolCode == null ? "" : symbolCode.trim().toUpperCase();
    }
}
