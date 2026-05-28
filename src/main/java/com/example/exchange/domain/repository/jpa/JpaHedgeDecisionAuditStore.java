/*
 * 檔案用途：JPA adapter，實作 hedge decision audit trail store。
 */
package com.example.exchange.domain.repository.jpa;

import com.example.exchange.domain.model.dto.HedgeDecisionAuditRecord;
import com.example.exchange.domain.model.entity.HedgeDecisionAuditRecordEntity;
import com.example.exchange.domain.repository.HedgeDecisionAuditStore;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class JpaHedgeDecisionAuditStore implements HedgeDecisionAuditStore {

    private final HedgeDecisionAuditRecordEntityJpaRepository repository;

    @Override
    @Transactional
    public void append(HedgeDecisionAuditRecord record) {
        repository.save(HedgeDecisionAuditRecordEntity.from(record, HedgeDecisionAuditStore.SCHEMA_VERSION));
    }

    @Override
    @Transactional(readOnly = true)
    public List<HedgeDecisionAuditRecord> findByMarketMakerId(String marketMakerId, int limit) {
        if (marketMakerId == null || marketMakerId.isBlank()) return List.of();
        int normalizedLimit = Math.min(Math.max(1, limit), 500);
        return repository.findByMarketMakerIdOrderByDecidedAtDescIdDesc(
                        marketMakerId.trim(),
                        PageRequest.of(0, normalizedLimit)
                )
                .stream()
                .map(HedgeDecisionAuditRecordEntity::toRecord)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<HedgeDecisionAuditRecord> findByRefId(String refId) {
        if (refId == null || refId.isBlank()) return List.of();
        return repository.findByRefIdOrderByDecidedAtAscIdAsc(refId.trim())
                .stream()
                .map(HedgeDecisionAuditRecordEntity::toRecord)
                .toList();
    }
}
