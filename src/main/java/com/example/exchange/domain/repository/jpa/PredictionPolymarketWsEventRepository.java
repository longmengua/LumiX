/*
 * 檔案用途：JPA Repository，提供 MySQL 持久化查詢與寫入能力。
 */
package com.example.exchange.domain.repository.jpa;

import com.example.exchange.domain.model.entity.PredictionPolymarketWsEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PredictionPolymarketWsEventRepository
        extends JpaRepository<PredictionPolymarketWsEvent, Long> {

    Optional<PredictionPolymarketWsEvent> findByEventKey(String eventKey);

    @Query("""
            select event from PredictionPolymarketWsEvent event
            where (:walletAddress is null or event.walletAddress = :walletAddress)
              and event.receivedAt is not null
              and (
                    event.receivedAt > :lastReceivedAt
                    or (event.receivedAt = :lastReceivedAt and event.eventKey > :lastEventKey)
              )
            order by event.receivedAt asc, event.eventKey asc
            """)
    List<PredictionPolymarketWsEvent> findReplayBatchAfterCheckpoint(
            @Param("walletAddress") String walletAddress,
            @Param("lastReceivedAt") LocalDateTime lastReceivedAt,
            @Param("lastEventKey") String lastEventKey,
            Pageable pageable
    );
}
