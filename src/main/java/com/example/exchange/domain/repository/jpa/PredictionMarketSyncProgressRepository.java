/*
 * 檔案用途：JPA Repository，提供 MySQL 持久化查詢與寫入能力。
 */
package com.example.exchange.domain.repository.jpa;

import com.example.exchange.domain.model.entity.PredictionMarketSyncProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PredictionMarketSyncProgressRepository
        extends JpaRepository<PredictionMarketSyncProgress, Long> {

    Optional<PredictionMarketSyncProgress> findByJobName(String jobName);
}