/*
 * 檔案用途：JPA adapter，實作 hedge execution worker lock。
 */
package com.example.exchange.domain.repository.jpa;

import com.example.exchange.domain.model.dto.HedgeExecutionLock;
import com.example.exchange.domain.model.entity.HedgeExecutionLockRecord;
import com.example.exchange.domain.repository.HedgeExecutionLockStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JpaHedgeExecutionLockStore implements HedgeExecutionLockStore {

    private final HedgeExecutionLockRecordJpaRepository repository;

    @Override
    @Transactional
    public Optional<HedgeExecutionLock> acquire(String lockName, String ownerId, Duration ttl, Instant now) {
        String normalizedLock = normalize(lockName);
        String normalizedOwner = normalize(ownerId);
        Instant expiresAt = now.plus(ttl == null || ttl.isNegative() || ttl.isZero()
                ? Duration.ofSeconds(30)
                : ttl);
        Optional<HedgeExecutionLockRecord> locked = repository.findLocked(normalizedLock);
        HedgeExecutionLockRecord record = locked.orElseGet(() -> {
            HedgeExecutionLockRecord created = new HedgeExecutionLockRecord();
            created.setLockName(normalizedLock);
            return created;
        });
        if (locked.isPresent()
                && record.getExpiresAt() != null
                && record.getExpiresAt().isAfter(now)
                && !normalizedOwner.equals(record.getOwnerId())) {
            return Optional.empty();
        }
        record.setOwnerId(normalizedOwner);
        record.setExpiresAt(expiresAt);
        record.setUpdatedAt(now);
        return Optional.of(repository.save(record).toLock());
    }

    @Override
    @Transactional
    public boolean release(String lockName, String ownerId, Instant now) {
        Optional<HedgeExecutionLockRecord> locked = repository.findLocked(normalize(lockName));
        if (locked.isEmpty() || !normalize(ownerId).equals(locked.get().getOwnerId())) {
            return false;
        }
        HedgeExecutionLockRecord record = locked.get();
        record.setExpiresAt(now);
        record.setUpdatedAt(now);
        repository.save(record);
        return true;
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? "default" : value.trim();
    }
}
