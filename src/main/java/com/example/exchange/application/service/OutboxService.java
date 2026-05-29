/*
 * 檔案用途：應用服務，編排領域模型、Repository 與外部基礎設施完成業務流程。
 */
package com.example.exchange.application.service;

import com.example.exchange.domain.model.entity.DlqEvent;
import com.example.exchange.domain.model.entity.OutboxEvent;
import com.example.exchange.domain.repository.DlqRepository;
import com.example.exchange.domain.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OutboxService {

    private static final int MAX_ATTEMPTS = 3;
    private static final Duration RETRY_DELAY = Duration.ofSeconds(30);

    private final OutboxRepository outboxRepository;
    private final DlqRepository dlqRepository;

    public void publish(String topic, String key, Object payload, CheckedPublisher publisher) {
        publish(topic, key, payload, Map.of(), publisher);
    }

    public void publish(String topic, String key, Object payload, Map<String, String> headers, CheckedPublisher publisher) {
        OutboxEvent event = OutboxEvent.builder()
                .topic(topic)
                .eventKey(key)
                .eventType(payload == null ? "null" : payload.getClass().getName())
                .payload(payload)
                .headers(normalizeHeaders(headers))
                .build();
        outboxRepository.save(event);
        publishAfterCommitOrNow(event, publisher);
    }

    public int relayDue(int limit, CheckedPublisher publisher) {
        int count = 0;
        for (OutboxEvent event : outboxRepository.findDue(Instant.now(), limit)) {
            if (publishExisting(event, publisher)) {
                count++;
            }
        }
        return count;
    }

    public List<DlqEvent> latestDlq(int limit) {
        return dlqRepository.latest(limit);
    }

    public OutboxEvent replayDead(UUID outboxId) {
        OutboxEvent event = outboxRepository.findById(outboxId)
                .orElseThrow(() -> new IllegalArgumentException("outbox event not found"));
        if (event.getStatus() != OutboxEvent.Status.DEAD) {
            throw new IllegalStateException("outbox event is not dead");
        }

        event.setStatus(OutboxEvent.Status.PENDING);
        event.setAttempts(0);
        event.setLastError(null);
        event.setNextAttemptAt(Instant.now());
        event.setPublishedAt(null);
        outboxRepository.save(event);
        return event;
    }

    public OutboxEvent markCompensated(UUID outboxId, String reason) {
        OutboxEvent event = outboxRepository.findById(outboxId)
                .orElseThrow(() -> new IllegalArgumentException("outbox event not found"));
        if (event.getStatus() != OutboxEvent.Status.DEAD) {
            throw new IllegalStateException("only dead outbox events can be compensated");
        }

        event.setStatus(OutboxEvent.Status.COMPENSATED);
        event.setLastError("COMPENSATED: " + normalizeReason(reason));
        event.setNextAttemptAt(null);
        outboxRepository.save(event);
        return event;
    }

    private boolean publishExisting(OutboxEvent event, CheckedPublisher publisher) {
        try {
            publisher.publish(event.getTopic(), event.getEventKey(), event.getPayload(), event.getHeaders());
            event.setStatus(OutboxEvent.Status.PUBLISHED);
            event.setPublishedAt(Instant.now());
            outboxRepository.save(event);
            return true;
        } catch (Exception ex) {
            markFailed(event, ex);
            return false;
        }
    }

    private void publishAfterCommitOrNow(OutboxEvent event, CheckedPublisher publisher) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            publishExisting(event, publisher);
            return;
        }

        // DB state and the outbox row must commit first; Kafka/external publish happens only after commit.
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                publishExisting(event, publisher);
            }
        });
    }

    private void markFailed(OutboxEvent event, Exception ex) {
        int attempts = event.getAttempts() + 1;
        event.setAttempts(attempts);
        event.setLastError(ex.getMessage());
        if (attempts >= MAX_ATTEMPTS) {
            event.setStatus(OutboxEvent.Status.DEAD);
            dlqRepository.append(DlqEvent.builder()
                    .outboxId(event.getId())
                    .topic(event.getTopic())
                    .eventKey(event.getEventKey())
                    .eventType(event.getEventType())
                    .payload(event.getPayload())
                    .attempts(attempts)
                    .error(ex.getMessage())
                    .build());
        } else {
            event.setNextAttemptAt(Instant.now().plus(RETRY_DELAY.multipliedBy(attempts)));
        }
        outboxRepository.save(event);
    }

    private static String normalizeReason(String reason) {
        if (reason == null || reason.isBlank()) return "manual";
        return reason.trim();
    }

    private static Map<String, String> normalizeHeaders(Map<String, String> headers) {
        if (headers == null || headers.isEmpty()) return Map.of();
        Map<String, String> normalized = new LinkedHashMap<>();
        headers.forEach((name, value) -> {
            if (name == null || name.isBlank() || value == null || value.isBlank()) return;
            normalized.put(name, value);
        });
        return Map.copyOf(normalized);
    }

    @FunctionalInterface
    public interface CheckedPublisher {
        void publish(String topic, String key, Object payload, Map<String, String> headers) throws Exception;
    }
}
