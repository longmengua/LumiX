/*
 * 檔案用途：JPA entity，保存 per-symbol matching sequencer lease。
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
@Table(name = "matching_sequencer_leases")
public class MatchingSequencerLeaseRecord {

    @Id
    @Column(name = "symbol_code", nullable = false, length = 32)
    private String symbolCode;

    @Column(name = "owner_id", nullable = false, length = 128)
    private String ownerId;

    @Column(name = "epoch", nullable = false)
    private Long epoch;

    @Column(name = "expires_at", nullable = false, columnDefinition = "TIMESTAMP(6)")
    private Instant expiresAt;

    @Column(name = "command_offset", nullable = false)
    private Long commandOffset;

    @Column(name = "event_offset", nullable = false)
    private Long eventOffset;

    @Column(name = "updated_at", nullable = false, columnDefinition = "TIMESTAMP(6)")
    private Instant updatedAt;
}
