package com.example.exchange.domain.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 世界杯賽事同步 Key。
 *
 * 這張表是 event 層資料。
 *
 * 例如：
 * Mexico vs South Africa
 * eventSlug = fifwc-mex-rsa-2026-06-11
 *
 * 注意：
 * eventSlug 不是 Polymarket market slug。
 * Polymarket market slug 是 outcome 層：
 * - fifwc-mex-rsa-2026-06-11-mex
 * - fifwc-mex-rsa-2026-06-11-draw
 * - fifwc-mex-rsa-2026-06-11-rsa
 */
@Getter
@Setter
@Entity
@Table(
        name = "prediction_market_sync_key",
        indexes = {
                @Index(name = "idx_pm_sync_enabled", columnList = "sync_enabled"),
                @Index(name = "idx_pm_sync_status", columnList = "sync_status"),
                @Index(name = "idx_pm_event_date", columnList = "event_date")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_pm_event_slug",
                        columnNames = "event_slug"
                )
        }
)
public class PredictionMarketSyncKeyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Polymarket event slug。
     *
     * 可以先為空。
     * 第一次 sync 時如果透過 fixture match 找到 event，
     * 就會回寫 eventSlug。
     */
    @Column(name = "event_slug", length = 128)
    private String eventSlug;

    /**
     * 前端顯示用 title。
     */
    @Column(name = "event_title", length = 256)
    private String eventTitle;

    /**
     * 主隊 / 左側隊伍。
     */
    @Column(name = "team_a", nullable = false, length = 128)
    private String teamA;

    /**
     * 客隊 / 右側隊伍。
     */
    @Column(name = "team_b", nullable = false, length = 128)
    private String teamB;

    /**
     * 比賽日期。
     */
    @Column(name = "event_date", nullable = false)
    private LocalDate eventDate;

    /**
     * 資料來源。
     *
     * 例如：
     * FIFA_2026
     */
    @Column(name = "source", length = 64)
    private String source;

    /**
     * 是否啟用同步。
     */
    @Column(name = "sync_enabled", nullable = false)
    private Boolean syncEnabled = true;

    /**
     * 單場同步狀態。
     *
     * PENDING / SUCCESS / FAILED
     */
    @Column(name = "sync_status", length = 32)
    private String syncStatus = "PENDING";

    /**
     * retry 次數。
     */
    @Column(name = "retry_count")
    private Integer retryCount = 0;

    /**
     * 最後錯誤。
     */
    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    /**
     * 最後成功同步 metadata 時間。
     */
    @Column(name = "last_synced_at")
    private LocalDateTime lastSyncedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();

        if (syncEnabled == null) {
            syncEnabled = true;
        }

        if (syncStatus == null) {
            syncStatus = "PENDING";
        }

        if (retryCount == null) {
            retryCount = 0;
        }

        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}