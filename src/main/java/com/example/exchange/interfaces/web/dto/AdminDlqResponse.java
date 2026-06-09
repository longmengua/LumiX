/*
 * File purpose: Admin DLQ response DTOs for read-only operations screens.
 */
package com.example.exchange.interfaces.web.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AdminDlqResponse(
        List<DlqItem> items,
        DlqCapabilities capabilities
) {

    public record DlqItem(
            UUID dlqId,
            UUID outboxId,
            String topic,
            String eventKey,
            String eventType,
            String status,
            int attempts,
            String errorSummary,
            String payloadPreview,
            String headerPreview,
            String requestId,
            String correlationId,
            boolean replayEligible,
            boolean compensationEligible,
            Instant createdAt,
            Instant outboxCreatedAt,
            Instant outboxUpdatedAt
    ) {
    }

    public record DlqCapabilities(
            boolean readOnly,
            boolean replayEnabled,
            boolean compensationEnabled,
            List<String> disabledActions,
            List<String> actionRequirements,
            List<String> backendEndpoints
    ) {
    }
}
