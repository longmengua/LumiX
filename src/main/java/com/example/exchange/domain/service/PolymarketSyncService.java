package com.example.exchange.domain.service;

import com.example.exchange.domain.model.dto.PredictionGammaMarketDto;
import com.example.exchange.domain.model.dto.CheckResult;
import com.example.exchange.domain.model.dto.SyncOneResult;
import com.example.exchange.domain.model.entity.PredictionMarketInfo;
import com.example.exchange.domain.model.entity.PredictionMarketSyncKey;
import com.example.exchange.domain.model.entity.PredictionMarketSyncProgress;
import com.example.exchange.domain.repository.client.PredictionGammaMarketClient;
import com.example.exchange.domain.repository.jpa.PredictionMarketInfoRepository;
import com.example.exchange.domain.repository.jpa.PredictionMarketSyncKeyRepository;
import com.example.exchange.domain.repository.jpa.PredictionMarketSyncProgressRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * Prediction Market Key Sync Service。
 *
 * 職責：
 * 1. 讀取 prediction_market_sync_key
 * 2. 對每個 key 用 teamA + teamB 查 Gamma search API
 * 3. 補齊 prediction_market_info 裡缺少的 homeWin / draw / awayWin
 * 4. 更新 sync key 狀態
 * 5. 更新 sync progress
 *
 * 注意：
 * 這裡不做 Gamma 全量 discovery。
 * 全量 discovery 請走：
 * POST /api/prediction/markets/discover
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PolymarketSyncService {

    private static final String JOB_NAME = "PREDICTION_MARKET_KEY_SYNC";

    private static final String STATUS_IDLE = "IDLE";
    private static final String STATUS_RUNNING = "RUNNING";
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_FAILED = "FAILED";

    private final AtomicBoolean running = new AtomicBoolean(false);

    private final PredictionGammaMarketClient gammaMarketClient;
    private final PolymarketMetadataSyncService metadataSyncService;
    private final PredictionMarketSyncKeyRepository syncKeyRepository;
    private final PredictionMarketSyncProgressRepository progressRepository;
    private final PredictionMarketInfoRepository marketInfoRepository;

    /**
     * Resume sync。
     *
     * 從 progress.lastSyncKeyId 後面繼續處理。
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
     * 這裡會重新 search Gamma，嘗試補齊該 event 的 outcome。
     */
    public String retryEvent(String eventSlug) {
        try {
            PredictionMarketSyncKey key =
                    syncKeyRepository.findByEventSlug(eventSlug)
                            .orElseThrow(() -> new IllegalArgumentException(
                                    "sync key not found, eventSlug=" + eventSlug
                            ));

            SyncOneResult result = syncOneKeyBySearch(key, true);

            if (result.isSuccess()) {
                return "retry sync success, eventSlug=" + eventSlug
                        + ", saved=" + result.getSavedCount();
            }

            return "retry sync failed, eventSlug=" + eventSlug
                    + ", error=" + result.getMessage();

        } catch (Exception e) {
            log.warn("retry event sync failed, eventSlug={}", eventSlug, e);
            return "retry sync failed, eventSlug=" + eventSlug
                    + ", error=" + e.getMessage();
        }
    }

    /**
     * 查詢同步進度。
     */
    public PredictionMarketSyncProgress getProgress() {
        return getOrCreateProgress();
    }

    /**
     * 核心同步流程。
     *
     * 這裡不全量拉 Gamma。
     * 每個 key 只用 teamA + teamB 做 search。
     */
    @Transactional
    protected void doSync(boolean reset) {
        PredictionMarketSyncProgress progress = getOrCreateProgress();

        Long lastSyncKeyId = reset
                ? 0L
                : safeLong(progress.getLastSyncKeyId());

        List<PredictionMarketSyncKey> keys =
                syncKeyRepository
                        .findBySyncEnabledTrueAndIdGreaterThanOrderByIdAsc(lastSyncKeyId);

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
            finishProgress(progress, 0);
            return;
        }

        int successCount = 0;
        int failedCount = 0;

        for (PredictionMarketSyncKey key : keys) {
            try {
                SyncOneResult result = syncOneKeyBySearch(key, false);

                if (result.isSuccess()) {
                    successCount++;
                } else {
                    failedCount++;
                }

                progress.setLastSyncKeyId(key.getId());
                progress.setSuccessCount(successCount);
                progress.setFailedCount(failedCount);
                progress.setUpdatedAt(LocalDateTime.now());

                if (!result.isSuccess()) {
                    progress.setLastError(
                            "eventSlug=" + key.getEventSlug()
                                    + ", error=" + result.getMessage()
                    );
                }

                progressRepository.save(progress);

                /**
                 * 避免 search API 打太快。
                 */
                sleepQuietly(300);

            } catch (Exception e) {
                failedCount++;

                key.setSyncStatus(STATUS_FAILED);
                key.setLastError(e.getMessage());
                syncKeyRepository.save(key);

                progress.setLastSyncKeyId(key.getId());
                progress.setSuccessCount(successCount);
                progress.setFailedCount(failedCount);
                progress.setLastError(
                        "eventSlug=" + key.getEventSlug()
                                + ", error=" + e.getMessage()
                );
                progress.setUpdatedAt(LocalDateTime.now());
                progressRepository.save(progress);

                log.warn("Prediction key sync failed, eventSlug={}", key.getEventSlug(), e);
            }
        }

        finishProgress(progress, failedCount);
    }

    /**
     * 用 Gamma search 同步單一 key。
     *
     * 流程：
     * 1. search teamA + teamB
     * 2. metadataSyncService 進行 match / classify / save
     * 3. 再檢查 DB 是否已經有完整 outcome
     *
     * @param increaseRetryCount 是否增加 retryCount，手動 retry 才增加
     */
    private SyncOneResult syncOneKeyBySearch(
            PredictionMarketSyncKey key,
            boolean increaseRetryCount
    ) {
        String keyword = buildSearchKeyword(key);

        List<PredictionGammaMarketDto> searchResult =
                gammaMarketClient.searchMarkets(keyword);

        int saved = 0;

        if (!searchResult.isEmpty()) {
            saved = metadataSyncService.syncOneKey(key, searchResult);
        }

        CheckResult checkResult = checkOneKey(key);

        applyCheckResult(key, checkResult, increaseRetryCount);

        syncKeyRepository.save(key);

        if (checkResult.isSuccess()) {
            return new SyncOneResult(true, saved, null);
        }

        String message = checkResult.getMessage();

        if (searchResult.isEmpty()) {
            message = "Gamma search empty, keyword=" + keyword;
        }

        return new SyncOneResult(false, saved, message);
    }

    /**
     * 建立 Gamma search keyword。
     *
     * 不使用 eventSlug。
     * 因為 eventSlug 是 event 層，不是 market slug。
     */
    private String buildSearchKeyword(PredictionMarketSyncKey key) {
        return safe(key.getTeamA()) + " " + safe(key.getTeamB());
    }

    /**
     * 檢查單一 key 是否已有完整 outcome。
     */
    private CheckResult checkOneKey(PredictionMarketSyncKey key) {
        if (key.getEventSlug() == null || key.getEventSlug().isBlank()) {
            return new CheckResult(false, "missing eventSlug");
        }

        List<PredictionMarketInfo> markets =
                marketInfoRepository.findByEventSlug(key.getEventSlug());

        Set<String> outcomeKeys =
                markets.stream()
                        .map(PredictionMarketInfo::getOutcomeKey)
                        .collect(Collectors.toSet());

        boolean hasHome = outcomeKeys.contains("homeWin");
        boolean hasDraw = outcomeKeys.contains("draw");
        boolean hasAway = outcomeKeys.contains("awayWin");

        if (hasHome && hasDraw && hasAway) {
            return new CheckResult(true, null);
        }

        StringBuilder missing = new StringBuilder();

        if (!hasHome) {
            missing.append("homeWin ");
        }

        if (!hasDraw) {
            missing.append("draw ");
        }

        if (!hasAway) {
            missing.append("awayWin ");
        }

        return new CheckResult(false, missing.toString().trim());
    }

    /**
     * 根據檢查結果更新 key。
     */
    private void applyCheckResult(
            PredictionMarketSyncKey key,
            CheckResult result,
            boolean increaseRetryCount
    ) {
        if (result.isSuccess()) {
            key.setSyncStatus(STATUS_SUCCESS);
            key.setLastError(null);
            key.setLastSyncedAt(LocalDateTime.now());
            return;
        }

        key.setSyncStatus(STATUS_FAILED);
        key.setLastError(result.getMessage());

        if (increaseRetryCount) {
            key.setRetryCount(safeInt(key.getRetryCount()) + 1);
        }
    }

    /**
     * 結束 progress。
     *
     * 注意：
     * 只要 job 跑完，就標 SUCCESS。
     * failedCount 用來表示有多少 event 還沒有補齊。
     */
    private void finishProgress(
            PredictionMarketSyncProgress progress,
            int failedCount
    ) {
        progress.setStatus(STATUS_SUCCESS);
        progress.setFinishedAt(LocalDateTime.now());
        progress.setUpdatedAt(LocalDateTime.now());

        if (failedCount > 0) {
            progress.setLastError("incomplete event count=" + failedCount);
        }

        progressRepository.save(progress);
    }

    /**
     * 重置 progress。
     */
    @Transactional
    public void resetProgress() {
        PredictionMarketSyncProgress progress = getOrCreateProgress();

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
     * 標記 job 程式異常失敗。
     */
    @Transactional
    public void markFailed(String error) {
        PredictionMarketSyncProgress progress = getOrCreateProgress();

        progress.setStatus(STATUS_FAILED);
        progress.setLastError(error);
        progress.setFinishedAt(LocalDateTime.now());
        progress.setUpdatedAt(LocalDateTime.now());

        progressRepository.save(progress);
    }

    /**
     * 查詢或初始化 progress。
     */
    private PredictionMarketSyncProgress getOrCreateProgress() {
        return progressRepository.findByJobName(JOB_NAME)
                .orElseGet(() -> {
                    PredictionMarketSyncProgress progress =
                            new PredictionMarketSyncProgress();

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

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}