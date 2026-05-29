/*
 * 檔案用途：JPA adapter，實作 hedge venue submit idempotency claim/result store。
 */
package com.example.exchange.domain.repository.jpa;

import com.example.exchange.domain.model.dto.HedgeOrderResult;
import com.example.exchange.domain.model.dto.HedgeVenueIdempotencyRecord;
import com.example.exchange.domain.model.entity.HedgeVenueIdempotencyRecordEntity;
import com.example.exchange.domain.repository.HedgeVenueIdempotencyStore;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JpaHedgeVenueIdempotencyStore implements HedgeVenueIdempotencyStore {

    private final HedgeVenueIdempotencyRecordRepository repository;

    @Override
    @Transactional(readOnly = true)
    public Optional<HedgeVenueIdempotencyRecord> find(String refId) {
        return repository.findByRefId(refId)
                .map(this::toRecord);
    }

    @Override
    @Transactional
    public boolean claim(String refId, String fingerprint) {
        HedgeVenueIdempotencyRecordEntity entity =
                new HedgeVenueIdempotencyRecordEntity();
        entity.setRefId(refId);
        entity.setFingerprint(fingerprint);
        entity.setCompleted(false);

        try {
            repository.saveAndFlush(entity);
            return true;
        } catch (DataIntegrityViolationException e) {
            return false;
        }
    }

    @Override
    @Transactional
    public HedgeVenueIdempotencyRecord complete(
            String refId,
            String fingerprint,
            HedgeOrderResult result
    ) {
        HedgeVenueIdempotencyRecordEntity entity =
                repository.findByRefId(refId)
                        .orElseThrow(() -> new IllegalStateException("hedge venue idempotency claim missing: " + refId));

        if (!entity.getFingerprint().equals(fingerprint)) {
            return toRecord(entity);
        }

        entity.setCompleted(true);
        entity.setAccepted(result.accepted());
        entity.setVenueOrderId(result.venueOrderId());
        entity.setReason(result.reason());
        entity.setRetryable(result.retryable());
        entity.setSubmittedAt(result.submittedAt());
        return toRecord(repository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public List<HedgeVenueIdempotencyRecord> findUnresolved(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 500));
        return repository.findByCompletedFalseOrRetryableTrueOrderByUpdatedAtAsc(PageRequest.of(0, safeLimit))
                .stream()
                .map(this::toRecord)
                .toList();
    }

    private HedgeVenueIdempotencyRecord toRecord(HedgeVenueIdempotencyRecordEntity entity) {
        HedgeOrderResult result = null;
        if (Boolean.TRUE.equals(entity.getCompleted())) {
            result = new HedgeOrderResult(
                    Boolean.TRUE.equals(entity.getAccepted()),
                    entity.getVenueOrderId(),
                    entity.getReason(),
                    Boolean.TRUE.equals(entity.getRetryable()),
                    entity.getSubmittedAt()
            );
        }

        return new HedgeVenueIdempotencyRecord(
                entity.getRefId(),
                entity.getFingerprint(),
                Boolean.TRUE.equals(entity.getCompleted()),
                result
        );
    }
}
