/*
 * 檔案用途：outbox/domain-state consistency issue DTO。
 */
package com.example.exchange.domain.model.dto;

import java.util.UUID;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;


@Data
@Builder
@Jacksonized
public class OutboxDomainStateConsistencyIssue {

    private final UUID outboxId;

    private final String topic;

    private final String eventKey;

    private final String eventType;

    private final String status;

    private final String issueType;

    private final String detail;
    public OutboxDomainStateConsistencyIssue(UUID outboxId, String topic, String eventKey, String eventType, String status, String issueType, String detail) {
        this.outboxId = outboxId;
        this.topic = topic;
        this.eventKey = eventKey;
        this.eventType = eventType;
        this.status = status;
        this.issueType = issueType;
        this.detail = detail;
    }

    public UUID outboxId() {
        return outboxId;
    }

    public String topic() {
        return topic;
    }

    public String eventKey() {
        return eventKey;
    }

    public String eventType() {
        return eventType;
    }

    public String status() {
        return status;
    }

    public String issueType() {
        return issueType;
    }

    public String detail() {
        return detail;
    }
}