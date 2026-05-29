/*
 * 檔案用途：JPA adapter，實作 hedge fill audit trail store。
 */
package com.example.exchange.domain.repository.jpa;

import com.example.exchange.domain.model.dto.HedgeFillRecord;
import com.example.exchange.domain.model.entity.HedgeFillRecordEntity;
import com.example.exchange.domain.repository.HedgeFillStore;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JpaHedgeFillStore implements HedgeFillStore {

    private final HedgeFillRecordEntityJpaRepository repository;

    @Override
    @Transactional
    public void append(HedgeFillRecord record) {
        try {
            repository.save(HedgeFillRecordEntity.from(record, HedgeFillStore.SCHEMA_VERSION));
        } catch (DataIntegrityViolationException ignored) {
            // Venue fill 回報可能重送；同一 venue_order_id + venue_fill_id 視為冪等。
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<HedgeFillRecord> findByVenueOrderIdAndVenueFillId(String venueOrderId, String venueFillId) {
        if (venueOrderId == null || venueOrderId.isBlank()
                || venueFillId == null || venueFillId.isBlank()) {
            return Optional.empty();
        }
        return repository.findByVenueOrderIdAndVenueFillId(venueOrderId.trim(), venueFillId.trim())
                .map(HedgeFillRecordEntity::toRecord);
    }

    @Override
    @Transactional(readOnly = true)
    public List<HedgeFillRecord> findByMarketMakerId(String marketMakerId, int limit) {
        if (marketMakerId == null || marketMakerId.isBlank()) return List.of();
        int normalizedLimit = Math.min(Math.max(1, limit), 500);
        return repository.findByMarketMakerIdOrderByFilledAtDescIdDesc(
                        marketMakerId.trim(),
                        PageRequest.of(0, normalizedLimit)
                )
                .stream()
                .map(HedgeFillRecordEntity::toRecord)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<HedgeFillRecord> findByVenueOrderId(String venueOrderId) {
        if (venueOrderId == null || venueOrderId.isBlank()) return List.of();
        return repository.findByVenueOrderIdOrderByFilledAtAscIdAsc(venueOrderId.trim())
                .stream()
                .map(HedgeFillRecordEntity::toRecord)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<HedgeFillRecord> findByRefId(String refId) {
        if (refId == null || refId.isBlank()) return List.of();
        return repository.findByRefIdOrderByFilledAtAscIdAsc(refId.trim())
                .stream()
                .map(HedgeFillRecordEntity::toRecord)
                .toList();
    }
}
