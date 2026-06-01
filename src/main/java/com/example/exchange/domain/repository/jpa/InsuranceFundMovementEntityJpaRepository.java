/*
 * 檔案用途：Spring Data JPA repository，查詢 insurance fund movement records。
 */
package com.example.exchange.domain.repository.jpa;

import com.example.exchange.domain.model.entity.InsuranceFundMovementEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InsuranceFundMovementEntityJpaRepository
        extends JpaRepository<InsuranceFundMovementEntity, String> {

    List<InsuranceFundMovementEntity> findByAssetOrderByCreatedAtDescMovementIdAsc(String asset, Pageable pageable);

    List<InsuranceFundMovementEntity> findAllByOrderByCreatedAtDescMovementIdAsc(Pageable pageable);
}
