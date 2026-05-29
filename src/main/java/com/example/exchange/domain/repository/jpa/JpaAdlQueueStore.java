/*
 * 檔案用途：JPA adapter，將 ADL queue entry 與 operator claim 狀態保存到資料庫。
 */
package com.example.exchange.domain.repository.jpa;

import com.example.exchange.domain.model.dto.AdlQueueEntry;
import com.example.exchange.domain.model.entity.AdlQueueEntryEntity;
import com.example.exchange.domain.repository.AdlQueueStore;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class JpaAdlQueueStore implements AdlQueueStore {

    private final AdlQueueEntryEntityJpaRepository repository;

    @Override
    @Transactional
    public boolean enqueueIfAbsent(AdlQueueEntry entry) {
        requireLiquidationId(entry.liquidationId());
        if (repository.existsById(entry.liquidationId().trim())) {
            return false;
        }
        try {
            repository.save(AdlQueueEntryEntity.from(entry, Instant.now()));
            return true;
        } catch (DataIntegrityViolationException duplicate) {
            return false;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdlQueueEntry> list() {
        return repository.findAllByOrderByCreatedAtAscLiquidationIdAsc().stream()
                .map(AdlQueueEntryEntity::toEntry)
                .toList();
    }

    @Override
    @Transactional
    public boolean complete(String liquidationId) {
        String normalizedId = requireLiquidationId(liquidationId);
        if (!repository.existsById(normalizedId)) {
            return false;
        }
        repository.deleteById(normalizedId);
        return true;
    }

    @Override
    @Transactional
    public AdlQueueEntry updateRemaining(String liquidationId, BigDecimal remainingAmount) {
        if (remainingAmount == null || remainingAmount.signum() <= 0) {
            complete(liquidationId);
            return null;
        }
        AdlQueueEntryEntity entity = find(liquidationId);
        entity.setAmount(remainingAmount);
        entity.setUpdatedAt(Instant.now());
        return repository.save(entity).toEntry();
    }

    @Override
    @Transactional
    public AdlQueueEntry claim(String liquidationId, String owner) {
        String normalizedOwner = requireOwner(owner);
        AdlQueueEntryEntity entity = find(liquidationId);
        if ("CLAIMED".equals(entity.getStatus()) && !normalizedOwner.equals(entity.getOwner())) {
            throw new IllegalStateException("ADL queue entry already claimed by " + entity.getOwner());
        }
        Instant now = Instant.now();
        entity.setStatus("CLAIMED");
        entity.setOwner(normalizedOwner);
        entity.setClaimedAt(now);
        entity.setUpdatedAt(now);
        return repository.save(entity).toEntry();
    }

    @Override
    @Transactional
    public AdlQueueEntry release(String liquidationId, String owner) {
        String normalizedOwner = requireOwner(owner);
        AdlQueueEntryEntity entity = find(liquidationId);
        if ("CLAIMED".equals(entity.getStatus()) && !normalizedOwner.equals(entity.getOwner())) {
            throw new IllegalStateException("ADL queue entry claimed by " + entity.getOwner());
        }
        entity.setStatus("OPEN");
        entity.setOwner("");
        entity.setClaimedAt(null);
        entity.setUpdatedAt(Instant.now());
        return repository.save(entity).toEntry();
    }

    private AdlQueueEntryEntity find(String liquidationId) {
        String normalizedId = requireLiquidationId(liquidationId);
        return repository.findById(normalizedId)
                .orElseThrow(() -> new IllegalArgumentException("ADL queue entry not found: " + liquidationId));
    }

    private static String requireLiquidationId(String liquidationId) {
        if (liquidationId == null || liquidationId.isBlank()) {
            throw new IllegalArgumentException("liquidationId must not be blank");
        }
        return liquidationId.trim();
    }

    private static String requireOwner(String owner) {
        if (owner == null || owner.isBlank()) {
            throw new IllegalArgumentException("ADL queue owner must not be blank");
        }
        return owner.trim();
    }
}
