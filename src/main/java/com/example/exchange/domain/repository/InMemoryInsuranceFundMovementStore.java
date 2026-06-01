/*
 * 檔案用途：測試與未配置資料庫時使用的 insurance fund movement store。
 */
package com.example.exchange.domain.repository;

import com.example.exchange.domain.model.dto.InsuranceFundMovement;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class InMemoryInsuranceFundMovementStore implements InsuranceFundMovementStore {

    private final List<InsuranceFundMovement> movements = new ArrayList<>();

    @Override
    public synchronized void save(InsuranceFundMovement movement) {
        movements.add(movement);
    }

    @Override
    public synchronized List<InsuranceFundMovement> findRecent(String asset, int limit) {
        String normalizedAsset = asset == null ? "" : asset.trim().toUpperCase();
        int safeLimit = Math.max(1, Math.min(limit, 500));
        return movements.stream()
                .filter(movement -> normalizedAsset.isBlank() || normalizedAsset.equals(movement.asset()))
                .sorted(Comparator.comparing(InsuranceFundMovement::createdAt).reversed()
                        .thenComparing(InsuranceFundMovement::movementId))
                .limit(safeLimit)
                .toList();
    }
}
