/*
 * 檔案用途：JPA adapter，實作 insurance fund capital movement durable store。
 */
package com.example.exchange.domain.repository.jpa;

import com.example.exchange.domain.model.dto.InsuranceFundMovement;
import com.example.exchange.domain.model.entity.InsuranceFundMovementEntity;
import com.example.exchange.domain.repository.InsuranceFundMovementStore;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class JpaInsuranceFundMovementStore implements InsuranceFundMovementStore {

    private final InsuranceFundMovementEntityJpaRepository repository;

    @Override
    @Transactional
    public void save(InsuranceFundMovement movement) {
        repository.save(InsuranceFundMovementEntity.from(movement));
    }

    @Override
    @Transactional(readOnly = true)
    public List<InsuranceFundMovement> findRecent(String asset, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 500));
        String normalizedAsset = asset == null ? "" : asset.trim().toUpperCase();
        List<InsuranceFundMovementEntity> records = normalizedAsset.isBlank()
                ? repository.findAllByOrderByCreatedAtDescMovementIdAsc(PageRequest.of(0, safeLimit))
                : repository.findByAssetOrderByCreatedAtDescMovementIdAsc(normalizedAsset, PageRequest.of(0, safeLimit));
        return records.stream().map(InsuranceFundMovementEntity::toMovement).toList();
    }
}
