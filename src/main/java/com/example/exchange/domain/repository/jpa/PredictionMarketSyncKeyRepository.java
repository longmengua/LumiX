package com.example.exchange.domain.repository.jpa;

import com.example.exchange.domain.model.entity.PredictionMarketSyncKeyEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PredictionMarketSyncKeyRepository
        extends JpaRepository<PredictionMarketSyncKeyEntity, Long> {

    List<PredictionMarketSyncKeyEntity> findBySyncEnabledTrueOrderByIdAsc();

    List<PredictionMarketSyncKeyEntity> findBySyncEnabledTrueAndIdGreaterThanOrderByIdAsc(
            Long id
    );

    Optional<PredictionMarketSyncKeyEntity> findByEventSlug(String eventSlug);

    Optional<PredictionMarketSyncKeyEntity> findByTeamAAndTeamBAndEventDate(
            String teamA,
            String teamB,
            java.time.LocalDate eventDate
    );
}
