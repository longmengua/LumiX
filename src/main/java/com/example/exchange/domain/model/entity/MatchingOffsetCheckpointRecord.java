/*
 * 檔案用途：JPA entity，保存 matching per-symbol durable offset checkpoint。
 */
package com.example.exchange.domain.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "matching_offset_checkpoints")
public class MatchingOffsetCheckpointRecord {

    @Id
    @Column(name = "symbol_code", nullable = false, length = 32)
    private String symbolCode;

    @Column(name = "command_offset", nullable = false)
    private Long commandOffset;

    @Column(name = "event_offset", nullable = false)
    private Long eventOffset;

    @Column(name = "updated_at", nullable = false, columnDefinition = "TIMESTAMP(6)")
    private Instant updatedAt;
}
