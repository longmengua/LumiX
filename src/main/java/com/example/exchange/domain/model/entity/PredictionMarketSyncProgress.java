/*
 * 檔案用途：領域模型或持久化實體，承載交易、帳戶、持倉與預測市場狀態。
 */
package com.example.exchange.domain.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 全量同步進度表。
 *
 * 用途：
 * - server 掛掉後可 resume
 * - 查詢目前 sync 進度
 */
@Getter
@Setter
@Entity
@Table(
        name = "prediction_market_sync_progress",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_pm_sync_progress_job",
                        columnNames = "job_name"
                )
        }
)
public class PredictionMarketSyncProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Job 名稱。
     */
    @Column(name = "job_name", nullable = false, length = 64)
    private String jobName;

    /**
     * 最後同步到哪一筆 sync key。
     */
    @Column(name = "last_sync_key_id")
    private Long lastSyncKeyId;

    /**
     * IDLE / RUNNING / SUCCESS / FAILED
     */
    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "total_count")
    private Integer totalCount;

    @Column(name = "success_count")
    private Integer successCount;

    @Column(name = "failed_count")
    private Integer failedCount;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (lastSyncKeyId == null) {
            lastSyncKeyId = 0L;
        }

        if (status == null) {
            status = "IDLE";
        }

        if (totalCount == null) {
            totalCount = 0;
        }

        if (successCount == null) {
            successCount = 0;
        }

        if (failedCount == null) {
            failedCount = 0;
        }

        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}