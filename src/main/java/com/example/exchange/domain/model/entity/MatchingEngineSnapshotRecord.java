/*
 * 檔案用途：JPA entity，保存 durable matching engine snapshot。
 */
package com.example.exchange.domain.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(
        name = "matching_engine_snapshots",
        indexes = {
                @Index(name = "idx_matching_snapshot_latest", columnList = "symbol_code,command_offset,event_offset,created_at")
        }
)
public class MatchingEngineSnapshotRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "symbol_code", nullable = false, length = 32)
    private String symbolCode;

    @Column(name = "match_sequence", nullable = false)
    private Long matchSequence;

    @Column(name = "command_offset", nullable = false)
    private Long commandOffset;

    @Column(name = "event_offset", nullable = false)
    private Long eventOffset;

    @Column(name = "snapshot_payload", nullable = false, columnDefinition = "JSON")
    private String snapshotPayload;

    @Column(name = "created_at", nullable = false, columnDefinition = "TIMESTAMP(6)")
    private Instant createdAt;
}
