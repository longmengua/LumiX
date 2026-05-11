package com.example.exchange.domain.service;

import com.example.exchange.domain.model.dto.PredictionGammaMarketDto;
import com.example.exchange.domain.model.entity.PredictionMarketSyncKeyEntity;
import com.example.exchange.domain.model.entity.PredictionMarketSyncProgressEntity;
import com.example.exchange.domain.repository.client.PredictionGammaMarketClient;
import com.example.exchange.domain.repository.jpa.PredictionMarketSyncKeyRepository;
import com.example.exchange.domain.repository.jpa.PredictionMarketSyncProgressRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Prediction Market Key Sync Service。
 *
 * 這個 Service 不負責 full discovery。
 *
 * 職責：
 * 1. 讀取 prediction_market_sync_key
 * 2. 逐筆同步已知 event
 * 3. 支援 resume
 * 4. 支援 reset
 * 5. 支援指定 eventSlug retry
 * 6. 記錄 sync progress
 *
 * 全量 discovery 請走：
 * PredictionMarketDiscoveryService
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PredictionMarketFullSyncService {

    private static final String JOB_NAME = "PREDICTION_MARKET_KEY_SYNC";

    private static final String STATUS_IDLE = "IDLE";
    private static final String STATUS_RUNNING = "RUNNING";
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_FAILED = "FAILED";

    /**
     * 防止同時重複執行 sync。
     */
    private final AtomicBoolean running = new AtomicBoolean(false);

    private final PredictionGammaMarketClient gammaMarketClient;
    private final PredictionMarketMetadataSyncService metadataSyncService;
    private final PredictionMarketSyncKeyRepository syncKeyRepository;
    private final PredictionMarketSyncProgressRepository progressRepository;

    /**
     * Resume sync。
     *
     * 從 progress.lastSyncKeyId 後面繼續跑。
     */
    public String syncResume() {
        if (!running.compareAndSet(false, true)) {
            return "Prediction market sync is already running";
        }

        try {
            doSync(false);
            return "Prediction market key sync finished";
        } catch (Exception e) {
            log.warn("Prediction market key sync failed", e);
            markFailed(e.getMessage());
            return "Prediction market key sync failed: " + e.getMessage();
        } finally {
            running.set(false);
        }
    }

    /**
     * Reset progress 後重新同步所有 key。
     */
    public String resetAndSync() {
        if (!running.compareAndSet(false, true)) {
            return "Prediction market sync is already running";
        }

        try {
            resetProgress();
            doSync(true);
            return "Prediction market key reset and sync finished";
        } catch (Exception e) {
            log.warn("Prediction market key reset and sync failed", e);
            markFailed(e.getMessage());
            return "Prediction market key reset and sync failed: " + e.getMessage();
        } finally {
            running.set(false);
        }
    }

    /**
     * 指定重試某場 event。
     *
     * 例如：
     * POST /api/prediction/markets/retry/fifwc-mex-rsa-2026-06-11
     */
    public String retryEvent(String eventSlug) {
        try {
            PredictionMarketSyncKeyEntity key =
                    syncKeyRepository.findByEventSlug(eventSlug)
                            .orElseThrow(() -> new IllegalArgumentException(
                                    "sync key not found, eventSlug=" + eventSlug
                            ));

            int saved = syncOneKeyWithFreshMarkets(key);

            if (saved <= 0) {
                key.setSyncStatus("FAILED");
                key.setLastError("no matched market");
                key.setRetryCount(safeInt(key.getRetryCount()) + 1);
                syncKeyRepository.save(key);

                return "retry failed, no matched market, eventSlug=" + eventSlug;
            }

            key.setSyncStatus("SUCCESS");
            key.setLastError(null);
            key.setLastSyncedAt(LocalDateTime.now());
            syncKeyRepository.save(key);

            return "retry success, eventSlug=" + eventSlug + ", saved=" + saved;

        } catch (Exception e) {
            log.warn("retry event failed, eventSlug={}", eventSlug, e);
            return "retry failed, eventSlug=" + eventSlug + ", error=" + e.getMessage();
        }
    }

    /**
     * 查詢同步進度。
     */
    public PredictionMarketSyncProgressEntity getProgress() {
        return getOrCreateProgress();
    }

    /**
     * 核心同步流程。
     *
     * reset=false：
     * - 從 lastSyncKeyId 後面繼續
     *
     * reset=true：
     * - 從 id > 0 開始
     *
     * 注意：
     * 這裡會拉一次 Gamma active markets，
     * 然後用這份資料同步所有 key。
     *
     * 這不是 discovery。
     * 它只處理 DB 裡已存在的 key。
     */
    @Transactional
    protected void doSync(boolean reset) {
        PredictionMarketSyncProgressEntity progress = getOrCreateProgress();

        Long lastSyncKeyId = reset
                ? 0L
                : safeLong(progress.getLastSyncKeyId());

        List<PredictionMarketSyncKeyEntity> keys =
                syncKeyRepository.findBySyncEnabledTrueAndIdGreaterThanOrderByIdAsc(lastSyncKeyId);

        progress.setStatus(STATUS_RUNNING);
        progress.setTotalCount(keys.size());
        progress.setSuccessCount(0);
        progress.setFailedCount(0);
        progress.setLastError(null);
        progress.setStartedAt(LocalDateTime.now());
        progress.setFinishedAt(null);
        progress.setUpdatedAt(LocalDateTime.now());
        progressRepository.save(progress);

        if (keys.isEmpty()) {
            progress.setStatus(STATUS_SUCCESS);
            progress.setFinishedAt(LocalDateTime.now());
            progress.setUpdatedAt(LocalDateTime.now());
            progressRepository.save(progress);
            return;
        }

        /**
         * 只拉一次 Gamma markets。
         *
         * 這裡是為了避免每個 key 都打一遍 Gamma API。
         */
        List<PredictionGammaMarketDto> allMarkets =
                gammaMarketClient.fetchAllActiveMarkets();

        int successCount = 0;
        int failedCount = 0;

        for (PredictionMarketSyncKeyEntity key : keys) {
            try {
                int saved = syncOneKeyWithRetry(key, allMarkets);

                if (saved > 0) {
                    successCount++;

                    key.setSyncStatus("SUCCESS");
                    key.setLastError(null);
                    key.setLastSyncedAt(LocalDateTime.now());
                } else {
                    failedCount++;

                    key.setSyncStatus("FAILED");
                    key.setLastError("no matched market");
                    key.setRetryCount(safeInt(key.getRetryCount()) + 1);
                }

                syncKeyRepository.save(key);

                progress.setLastSyncKeyId(key.getId());
                progress.setSuccessCount(successCount);
                progress.setFailedCount(failedCount);
                progress.setUpdatedAt(LocalDateTime.now());
                progressRepository.save(progress);

            } catch (Exception e) {
                failedCount++;

                key.setSyncStatus("FAILED");
                key.setLastError(e.getMessage());
                key.setRetryCount(safeInt(key.getRetryCount()) + 1);
                syncKeyRepository.save(key);

                progress.setLastSyncKeyId(key.getId());
                progress.setSuccessCount(successCount);
                progress.setFailedCount(failedCount);
                progress.setLastError(
                        "eventSlug=" + key.getEventSlug() + ", error=" + e.getMessage()
                );
                progress.setUpdatedAt(LocalDateTime.now());
                progressRepository.save(progress);

                log.warn(
                        "Prediction key sync failed, id={}, eventSlug={}",
                        key.getId(),
                        key.getEventSlug(),
                        e
                );
            }
        }

        progress.setStatus(failedCount > 0 ? STATUS_FAILED : STATUS_SUCCESS);
        progress.setFinishedAt(LocalDateTime.now());
        progress.setUpdatedAt(LocalDateTime.now());
        progressRepository.save(progress);
    }

    /**
     * 單一 key sync，重試 3 次。
     */
    private int syncOneKeyWithRetry(
            PredictionMarketSyncKeyEntity key,
            List<PredictionGammaMarketDto> allMarkets
    ) {
        RuntimeException lastException = null;

        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                return metadataSyncService.syncOneKey(key, allMarkets);
            } catch (RuntimeException e) {
                lastException = e;

                log.warn(
                        "sync key retry failed, eventSlug={}, attempt={}",
                        key.getEventSlug(),
                        attempt,
                        e
                );

                sleepQuietly(attempt * 1000L);
            }
        }

        throw lastException == null
                ? new RuntimeException("sync key failed")
                : lastException;
    }

    /**
     * 指定 retry 用。
     *
     * 每次 retry event 時，重新拉一份最新 Gamma markets。
     */
    private int syncOneKeyWithFreshMarkets(
            PredictionMarketSyncKeyEntity key
    ) {
        List<PredictionGammaMarketDto> allMarkets =
                gammaMarketClient.fetchAllActiveMarkets();

        return metadataSyncService.syncOneKey(key, allMarkets);
    }

    /**
     * 重置 progress。
     */
    @Transactional
    public void resetProgress() {
        PredictionMarketSyncProgressEntity progress = getOrCreateProgress();

        progress.setLastSyncKeyId(0L);
        progress.setStatus(STATUS_IDLE);
        progress.setTotalCount(0);
        progress.setSuccessCount(0);
        progress.setFailedCount(0);
        progress.setLastError(null);
        progress.setStartedAt(null);
        progress.setFinishedAt(null);
        progress.setUpdatedAt(LocalDateTime.now());

        progressRepository.save(progress);
    }

    /**
     * 標記 job failed。
     */
    @Transactional
    public void markFailed(String error) {
        PredictionMarketSyncProgressEntity progress = getOrCreateProgress();

        progress.setStatus(STATUS_FAILED);
        progress.setLastError(error);
        progress.setFinishedAt(LocalDateTime.now());
        progress.setUpdatedAt(LocalDateTime.now());

        progressRepository.save(progress);
    }

    /**
     * 查詢或初始化 progress。
     */
    private PredictionMarketSyncProgressEntity getOrCreateProgress() {
        return progressRepository.findByJobName(JOB_NAME)
                .orElseGet(() -> {
                    PredictionMarketSyncProgressEntity progress =
                            new PredictionMarketSyncProgressEntity();

                    progress.setJobName(JOB_NAME);
                    progress.setLastSyncKeyId(0L);
                    progress.setStatus(STATUS_IDLE);
                    progress.setTotalCount(0);
                    progress.setSuccessCount(0);
                    progress.setFailedCount(0);
                    progress.setUpdatedAt(LocalDateTime.now());

                    return progressRepository.save(progress);
                });
    }

    private Long safeLong(Long value) {
        return value == null ? 0L : value;
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}