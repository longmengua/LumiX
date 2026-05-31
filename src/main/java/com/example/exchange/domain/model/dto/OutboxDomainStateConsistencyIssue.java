/*
 * 檔案用途：outbox/domain-state consistency issue DTO。
 */
package com.example.exchange.domain.model.dto;

import java.util.UUID;

public record OutboxDomainStateConsistencyIssue(
        UUID outboxId,
        String topic,
        String eventKey,
        String eventType,
        String status,
        String issueType,
        String detail
) {
}
