/*
 * 檔案用途：JPA adapter，實作 production durable DLQ repository。
 */
package com.example.exchange.domain.repository.jpa;

import com.example.exchange.domain.model.entity.DlqEvent;
import com.example.exchange.domain.model.entity.DlqEventRecord;
import com.example.exchange.domain.repository.DlqRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Primary
@Repository
@RequiredArgsConstructor
public class JpaDlqRepository implements DlqRepository {

    private final DlqEventRecordJpaRepository repository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void append(DlqEvent event) {
        repository.save(toRecord(event));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DlqEvent> latest(int limit) {
        return repository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, Math.max(1, limit)))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    private DlqEventRecord toRecord(DlqEvent event) {
        DlqEventRecord record = new DlqEventRecord();
        record.setId(event.getId().toString());
        record.setOutboxId(event.getOutboxId() == null ? null : event.getOutboxId().toString());
        record.setTopic(event.getTopic());
        record.setEventKey(event.getEventKey());
        record.setEventType(event.getEventType());
        record.setPayload(writeJson(event.getPayload()));
        record.setAttempts(event.getAttempts());
        record.setError(event.getError());
        record.setCreatedAt(event.getCreatedAt());
        return record;
    }

    private DlqEvent toDomain(DlqEventRecord record) {
        return DlqEvent.builder()
                .id(UUID.fromString(record.getId()))
                .outboxId(record.getOutboxId() == null ? null : UUID.fromString(record.getOutboxId()))
                .topic(record.getTopic())
                .eventKey(record.getEventKey())
                .eventType(record.getEventType())
                .payload(readPayload(record.getPayload()))
                .attempts(record.getAttempts())
                .error(record.getError())
                .createdAt(record.getCreatedAt())
                .build();
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("serialize dlq json failed", e);
        }
    }

    private Object readPayload(String json) {
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (Exception e) {
            throw new IllegalStateException("deserialize dlq payload failed", e);
        }
    }
}
