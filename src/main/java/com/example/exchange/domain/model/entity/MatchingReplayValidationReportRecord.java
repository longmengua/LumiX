/*
 * 檔案用途：JPA entity，保存 matching replay validation report。
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
        name = "matching_replay_validation_reports",
        indexes = {
                @Index(name = "idx_matching_replay_report_symbol_time", columnList = "symbol_code,validated_at"),
                @Index(name = "idx_matching_replay_report_valid", columnList = "valid,validated_at")
        }
)
public class MatchingReplayValidationReportRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "symbol_code", nullable = false, length = 32)
    private String symbolCode;

    @Column(name = "valid", nullable = false)
    private Boolean valid;

    @Column(name = "start_command_offset", nullable = false)
    private Long startCommandOffset;

    @Column(name = "expected_command_offset", nullable = false)
    private Long expectedCommandOffset;

    @Column(name = "actual_command_offset", nullable = false)
    private Long actualCommandOffset;

    @Column(name = "expected_event_offset", nullable = false)
    private Long expectedEventOffset;

    @Column(name = "actual_event_offset", nullable = false)
    private Long actualEventOffset;

    @Column(name = "expected_match_sequence", nullable = false)
    private Long expectedMatchSequence;

    @Column(name = "actual_match_sequence", nullable = false)
    private Long actualMatchSequence;

    @Column(name = "issues_payload", nullable = false, columnDefinition = "JSON")
    private String issuesPayload;

    @Column(name = "validated_at", nullable = false, columnDefinition = "TIMESTAMP(6)")
    private Instant validatedAt;
}
