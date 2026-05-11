package com.example.exchange.domain.repository.jpa;

import com.example.exchange.domain.model.entity.PredictionMarketSyncProgressEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PredictionMarketSyncProgressRepository
        extends JpaRepository<PredictionMarketSyncProgressEntity, Long> {

    Optional<PredictionMarketSyncProgressEntity> findByJobName(String jobName);
}