package com.example.exchange.domain.repository.jpa;

import com.example.exchange.domain.model.entity.PredictionPolymarketWsEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PredictionPolymarketWsEventRepository
        extends JpaRepository<PredictionPolymarketWsEvent, Long> {

    Optional<PredictionPolymarketWsEvent> findByEventKey(String eventKey);
}
