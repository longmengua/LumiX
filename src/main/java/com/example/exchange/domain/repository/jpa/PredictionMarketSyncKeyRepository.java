package com.example.exchange.domain.repository.jpa;

import com.example.exchange.domain.model.entity.PredictionMarketSyncKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PredictionMarketSyncKeyRepository
        extends JpaRepository<PredictionMarketSyncKey, Long> {

    List<PredictionMarketSyncKey> findBySyncEnabledTrueOrderByIdAsc();

    List<PredictionMarketSyncKey> findBySyncEnabledTrueOrderByEventDateAsc();

    List<PredictionMarketSyncKey> findBySyncEnabledTrueAndIdGreaterThanOrderByIdAsc(
            Long id
    );

    List<PredictionMarketSyncKey> findBySyncEnabledTrueAndSyncStatusNotOrderByIdAsc(
            String syncStatus
    );

    Optional<PredictionMarketSyncKey> findByEventSlug(String eventSlug);

    Optional<PredictionMarketSyncKey> findByTeamAAndTeamBAndEventDate(
            String teamA,
            String teamB,
            java.time.LocalDate eventDate
    );
}
