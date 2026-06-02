/*
 * 檔案用途：Spring Data repository，保存 Polymarket user WebSocket durable checkpoint。
 */
package com.example.exchange.domain.repository.jpa;

import com.example.exchange.domain.model.entity.PredictionPolymarketUserWsCheckpoint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PredictionPolymarketUserWsCheckpointRepository
        extends JpaRepository<PredictionPolymarketUserWsCheckpoint, Long> {

    Optional<PredictionPolymarketUserWsCheckpoint> findByStreamKey(String streamKey);
}
