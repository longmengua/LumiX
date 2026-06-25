/*
 * File purpose: Read-only admin API for inspecting DLQ records with sanitized payload previews.
 */
package com.example.exchange.interfaces.web.controller;

import com.example.exchange.application.service.OutboxService;
import com.example.exchange.domain.model.dto.DlqEvent;
import com.example.exchange.domain.model.dto.OutboxEvent;
import com.example.exchange.domain.repository.OutboxRepository;
import com.example.exchange.domain.util.SensitiveLogSanitizer;
import com.example.exchange.infra.tracing.TraceContext;
import com.example.exchange.interfaces.web.dto.AdminDlqResponse;
import com.example.exchange.interfaces.web.dto.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin/dlq")
@RequiredArgsConstructor
public class AdminDlqController {

    private static final int PREVIEW_LIMIT = 1_200;
    private static final AdminDlqResponse.DlqCapabilities CAPABILITIES =
            new AdminDlqResponse.DlqCapabilities(
                    true,
                    false,
                    false,
                    List.of("replay-dead-outbox", "mark-compensated"),
                    List.of("operator role", "reason text", "confirmation", "visible request id"),
                    List.of(
                            "POST /api/recovery/outbox/dead/{outboxId}/replay",
                            "POST /api/recovery/outbox/dead/{outboxId}/compensate?reason=..."
                    )
            );

    private final OutboxService outboxService;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @GetMapping
    public ApiResponse<AdminDlqResponse> list(@RequestParam(defaultValue = "50") int limit) {
        List<AdminDlqResponse.DlqItem> items = outboxService.latestDlq(limit).stream()
                .map(this::toItem)
                .toList();
        return ApiResponse.ok(new AdminDlqResponse(items, CAPABILITIES));
    }

    private AdminDlqResponse.DlqItem toItem(DlqEvent event) {
        Optional<OutboxEvent> outbox = event.getOutboxId() == null
                ? Optional.empty()
                : outboxRepository.findById(event.getOutboxId());
        Map<String, String> headers = outbox.map(OutboxEvent::getHeaders).orElse(Map.of());
        String status = outbox.map(value -> value.getStatus().name()).orElse("DLQ_ONLY");
        boolean actionEligible = outbox
                .map(value -> value.getStatus() == OutboxEvent.Status.DEAD)
                .orElse(false);

        return new AdminDlqResponse.DlqItem(
                event.getId(),
                event.getOutboxId(),
                event.getTopic(),
                event.getEventKey(),
                event.getEventType(),
                status,
                event.getAttempts(),
                preview(event.getError()),
                previewJson(event.getPayload()),
                previewJson(headers),
                headers.get(TraceContext.REQUEST_ID_HEADER),
                headers.get(TraceContext.CORRELATION_ID_HEADER),
                actionEligible,
                actionEligible,
                event.getCreatedAt(),
                outbox.map(OutboxEvent::getCreatedAt).orElse(null),
                outbox.map(this::updatedAt).orElse(null)
        );
    }

    private Instant updatedAt(OutboxEvent event) {
        if (event.getPublishedAt() != null) {
            return event.getPublishedAt();
        }
        if (event.getNextAttemptAt() != null) {
            return event.getNextAttemptAt();
        }
        return event.getCreatedAt();
    }

    private String previewJson(Object value) {
        try {
            return preview(objectMapper.writeValueAsString(value));
        } catch (Exception e) {
            return "UNSERIALIZABLE_PAYLOAD";
        }
    }

    private String preview(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String sanitized = SensitiveLogSanitizer.sanitize(value.trim());
        return sanitized.length() <= PREVIEW_LIMIT
                ? sanitized
                : sanitized.substring(0, PREVIEW_LIMIT) + "...";
    }
}
