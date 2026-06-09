/*
 * File purpose: Verify read-only admin DLQ API mapping and payload redaction.
 */
package com.example.exchange.interfaces.web.controller;

import com.example.exchange.application.service.OutboxService;
import com.example.exchange.domain.model.entity.DlqEvent;
import com.example.exchange.domain.model.entity.OutboxEvent;
import com.example.exchange.domain.repository.DlqRepository;
import com.example.exchange.domain.repository.OutboxRepository;
import com.example.exchange.infra.tracing.TraceContext;
import com.example.exchange.interfaces.web.dto.AdminDlqResponse;
import com.example.exchange.interfaces.web.dto.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AdminDlqControllerTest {

    @Test
    @DisplayName("admin DLQ API returns sanitized read-only rows with replay eligibility")
    /**
     * Scenario: a DLQ row references a DEAD outbox row with sensitive payload/headers.
     * Expectation: admin response redacts secrets, exposes trace ids, and keeps replay actions disabled.
     */
    void returnsSanitizedDlqRows() {
        UUID outboxId = UUID.fromString("00000000-0000-0000-0000-000000000101");
        OutboxEvent outbox = OutboxEvent.builder()
                .id(outboxId)
                .topic("order.lifecycle")
                .eventKey("order-1")
                .eventType("OrderLifecycle")
                .payload(Map.of("apiKey", "secret-key", "orderId", "order-1"))
                .headers(Map.of(
                        TraceContext.REQUEST_ID_HEADER, "req-1",
                        TraceContext.CORRELATION_ID_HEADER, "corr-1",
                        "Authorization", "Bearer token-1"
                ))
                .status(OutboxEvent.Status.DEAD)
                .attempts(3)
                .lastError("apiSecret=secret-value")
                .createdAt(Instant.parse("2026-06-01T00:00:00Z"))
                .nextAttemptAt(Instant.parse("2026-06-01T00:01:00Z"))
                .build();
        DlqEvent dlq = DlqEvent.builder()
                .id(UUID.fromString("00000000-0000-0000-0000-000000000202"))
                .outboxId(outboxId)
                .topic("order.lifecycle")
                .eventKey("order-1")
                .eventType("OrderLifecycle")
                .payload(new LinkedHashMap<>(Map.of("apiSecret", "secret-value", "orderId", "order-1")))
                .attempts(3)
                .error("Authorization=Bearer token-2")
                .createdAt(Instant.parse("2026-06-01T00:02:00Z"))
                .build();
        StubOutboxRepository outboxRepository = new StubOutboxRepository(List.of(outbox));
        OutboxService outboxService = new OutboxService(outboxRepository, new StubDlqRepository(List.of(dlq)));
        AdminDlqController controller =
                new AdminDlqController(outboxService, outboxRepository, new ObjectMapper());

        ApiResponse<AdminDlqResponse> response = controller.list(10);

        assertThat(response.isOk()).isTrue();
        AdminDlqResponse.DlqItem item = response.getData().items().getFirst();
        assertThat(item.status()).isEqualTo("DEAD");
        assertThat(item.replayEligible()).isTrue();
        assertThat(item.compensationEligible()).isTrue();
        assertThat(item.requestId()).isEqualTo("req-1");
        assertThat(item.correlationId()).isEqualTo("corr-1");
        assertThat(item.payloadPreview()).contains("\"apiSecret\":\"***\"");
        assertThat(item.errorSummary()).contains("Authorization=***");
        assertThat(item.headerPreview()).contains("\"Authorization\":\"***\"");
        assertThat(response.getData().capabilities().readOnly()).isTrue();
        assertThat(response.getData().capabilities().replayEnabled()).isFalse();
        assertThat(response.getData().capabilities().actionRequirements()).contains("reason text");
    }

    private record StubDlqRepository(List<DlqEvent> events) implements DlqRepository {
        @Override
        public void append(DlqEvent event) {
        }

        @Override
        public List<DlqEvent> latest(int limit) {
            return events.stream().limit(limit).toList();
        }
    }

    private record StubOutboxRepository(List<OutboxEvent> events) implements OutboxRepository {
        @Override
        public void save(OutboxEvent event) {
        }

        @Override
        public Optional<OutboxEvent> findById(UUID id) {
            return events.stream()
                    .filter(event -> event.getId().equals(id))
                    .findFirst();
        }

        @Override
        public List<OutboxEvent> findDue(Instant now, int limit) {
            return List.of();
        }
    }
}
