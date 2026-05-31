/*
 * 檔案用途：JPA adapter，實作 production durable outbox repository。
 */
package com.example.exchange.domain.repository.jpa;

import com.example.exchange.domain.model.entity.OutboxEvent;
import com.example.exchange.domain.model.entity.OutboxEventRecord;
import com.example.exchange.domain.repository.OutboxRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import java.time.Instant;
import java.util.List;

@Primary
@Repository
@RequiredArgsConstructor
public class JpaOutboxRepository implements OutboxRepository {

    private static final TypeReference<Map<String, String>> STRING_MAP =
            new TypeReference<>() {
            };

    private final OutboxEventRecordJpaRepository repository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void save(OutboxEvent event) {
        repository.save(toRecord(event));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OutboxEvent> findById(UUID id) {
        if (id == null) return Optional.empty();
        return repository.findById(id.toString()).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OutboxEvent> findDue(Instant now, int limit) {
        return repository.findDue(now, PageRequest.of(0, Math.max(1, limit)))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OutboxEvent> latest(int limit) {
        return repository.findByOrderByCreatedAtDesc(PageRequest.of(0, Math.max(1, limit)))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    private OutboxEventRecord toRecord(OutboxEvent event) {
        OutboxEventRecord record = new OutboxEventRecord();
        record.setId(event.getId().toString());
        record.setTopic(event.getTopic());
        record.setEventKey(event.getEventKey());
        record.setEventType(event.getEventType());
        record.setPayload(writeJson(event.getPayload()));
        record.setHeaders(writeJson(event.getHeaders() == null ? Map.of() : event.getHeaders()));
        record.setStatus(event.getStatus().name());
        record.setAttempts(event.getAttempts());
        record.setLastError(event.getLastError());
        record.setCreatedAt(event.getCreatedAt());
        record.setNextAttemptAt(event.getNextAttemptAt());
        record.setPublishedAt(event.getPublishedAt());
        return record;
    }

    private OutboxEvent toDomain(OutboxEventRecord record) {
        return OutboxEvent.builder()
                .id(UUID.fromString(record.getId()))
                .topic(record.getTopic())
                .eventKey(record.getEventKey())
                .eventType(record.getEventType())
                .payload(readPayload(record.getPayload()))
                .headers(readHeaders(record.getHeaders()))
                .status(OutboxEvent.Status.valueOf(record.getStatus()))
                .attempts(record.getAttempts())
                .lastError(record.getLastError())
                .createdAt(record.getCreatedAt())
                .nextAttemptAt(record.getNextAttemptAt())
                .publishedAt(record.getPublishedAt())
                .build();
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("serialize outbox json failed", e);
        }
    }

    private Object readPayload(String json) {
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (Exception e) {
            throw new IllegalStateException("deserialize outbox payload failed", e);
        }
    }

    private Map<String, String> readHeaders(String json) {
        if (json == null || json.isBlank()) return Map.of();
        try {
            return objectMapper.readValue(json, STRING_MAP);
        } catch (Exception e) {
            throw new IllegalStateException("deserialize outbox headers failed", e);
        }
    }
}
