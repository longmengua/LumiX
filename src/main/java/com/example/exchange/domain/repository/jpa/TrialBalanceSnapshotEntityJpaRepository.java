/*
 * 檔案用途：Spring Data JPA repository，提供 trial balance snapshot 查詢。
 */
package com.example.exchange.domain.repository.jpa;

import com.example.exchange.domain.model.entity.TrialBalanceSnapshotEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface TrialBalanceSnapshotEntityJpaRepository extends JpaRepository<TrialBalanceSnapshotEntity, String> {

    Optional<TrialBalanceSnapshotEntity> findByReportDateAndUidAndAsset(LocalDate reportDate, long uid, String asset);
}
