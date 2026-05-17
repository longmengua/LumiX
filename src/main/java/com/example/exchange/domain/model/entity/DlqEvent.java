package com.example.exchange.domain.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
public class DlqEvent {

    @Builder.Default
    private UUID id = UUID.randomUUID();

    private UUID outboxId;
    private String topic;
    private String eventKey;
    private String eventType;
    private Object payload;
    private int attempts;
    private String error;

    @Builder.Default
    private Instant createdAt = Instant.now();
}
