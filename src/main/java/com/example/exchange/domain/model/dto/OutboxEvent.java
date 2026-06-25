/*
 * 檔案用途：領域模型或持久化實體，承載交易、帳戶、持倉與預測市場狀態。
 */
package com.example.exchange.domain.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
public class OutboxEvent {

    public enum Status {
        PENDING,
        PUBLISHED,
        DEAD,
        COMPENSATED
    }

    @Builder.Default
    private UUID id = UUID.randomUUID();

    private String topic;
    private String eventKey;
    private String eventType;
    private Object payload;
    @Builder.Default
    private Map<String, String> headers = Map.of();

    @Builder.Default
    private Status status = Status.PENDING;

    @Builder.Default
    private int attempts = 0;

    private String lastError;

    @Builder.Default
    private Instant createdAt = Instant.now();

    @Builder.Default
    private Instant nextAttemptAt = Instant.now();

    private Instant publishedAt;
}
