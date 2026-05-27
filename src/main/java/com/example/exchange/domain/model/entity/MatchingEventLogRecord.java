/*
 * 檔案用途：JPA entity，保存 durable matching event log。
 */
package com.example.exchange.domain.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(
        name = "matching_event_logs",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_matching_event_symbol_offset", columnNames = {"symbol_code", "offset_value"})
        },
        indexes = {
                @Index(name = "idx_matching_event_command_offset", columnList = "symbol_code,command_offset"),
                @Index(name = "idx_matching_event_symbol_created", columnList = "symbol_code,created_at")
        }
)
public class MatchingEventLogRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "symbol_code", nullable = false, length = 32)
    private String symbolCode;

    @Column(name = "offset_value", nullable = false)
    private Long offsetValue;

    @Column(name = "command_offset", nullable = false)
    private Long commandOffset;

    @Column(name = "trade_payload", nullable = false, columnDefinition = "JSON")
    private String tradePayload;

    @Column(name = "created_at", nullable = false, columnDefinition = "TIMESTAMP(6)")
    private Instant createdAt;
}
