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

import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class OutboxService {

    private static final int MAX_ATTEMPTS = 3;
    private static final Duration RETRY_DELAY = Duration.ofSeconds(30);

    private final OutboxRepository outboxRepository;
    private final DlqRepository dlqRepository;

    public void publish(String topic, String key, Object payload, CheckedPublisher publisher) {
        OutboxEvent event = OutboxEvent.builder()
                .topic(topic)
                .eventKey(key)
                .eventType(payload == null ? "null" : payload.getClass().getName())
                .payload(payload)
                .build();
        outboxRepository.save(event);
        publishExisting(event, publisher);
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

    private boolean publishExisting(OutboxEvent event, CheckedPublisher publisher) {
        try {
            publisher.publish(event.getTopic(), event.getEventKey(), event.getPayload());
            event.setStatus(OutboxEvent.Status.PUBLISHED);
            event.setPublishedAt(Instant.now());
            outboxRepository.save(event);
            return true;
        } catch (Exception ex) {
            markFailed(event, ex);
            return false;
        }
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

    @FunctionalInterface
    public interface CheckedPublisher {
        void publish(String topic, String key, Object payload) throws Exception;
    }
}
