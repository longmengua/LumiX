package com.example.exchange.domain.repository;

import com.example.exchange.domain.model.entity.OutboxEvent;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OutboxRepository {

    void save(OutboxEvent event);

    Optional<OutboxEvent> findById(UUID id);

    List<OutboxEvent> findDue(Instant now, int limit);
}
