package com.example.exchange.domain.service;

import com.example.exchange.domain.model.dto.PredictionGammaMarketDto;
import com.example.exchange.domain.model.entity.PredictionMarketInfo;
import com.example.exchange.domain.repository.client.PredictionGammaMarketClient;
import com.example.exchange.domain.repository.jpa.PredictionMarketInfoRepository;
import com.example.exchange.domain.util.PredictionJsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Prediction Market 價格刷新服務。
 *
 * 職責：
 * 1. 每 5 秒刷新價格
 * 2. 只更新熱資料（價格）
 * 3. 不更新 metadata
 * 4. 不重新 discovery
 *
 * 更新欄位：
 * - bestBid
 * - bestAsk
 * - lastTradePrice
 * - liquidity
 * - volume
 * - volume24hr
 * - outcomePrices
 * - clobTokenIds
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PolymarketPriceService {

    /**
     * 價格刷新最小間隔。
     *
     * 避免：
     * 同一 market 在短時間內被重複刷新。
     */
    private static final long PRICE_REFRESH_SECONDS = 5;

    private final PredictionGammaMarketClient gammaMarketClient;
    private final PredictionMarketInfoRepository marketInfoRepository;

    /**
     * 刷新價格。
     *
     * 流程：
     * 1. 找出需要更新的 markets
     * 2. Gamma 全量拉 active/open markets
     * 3. 用 marketSlug match
     * 4. 更新價格欄位
     */
    @Transactional
    public void refreshPrices() {
        LocalDateTime now = LocalDateTime.now();

        List<PredictionMarketInfo> markets =
                marketInfoRepository.findByActiveTrueAndClosedFalseOrderByEventDateAsc();

        if (markets.isEmpty()) {
            log.info("Prediction price refresh skipped, no active markets");
            return;
        }

        int updated = 0;
        int failed = 0;

        for (PredictionMarketInfo entity : markets) {
            if (entity.getMarketSlug() == null || entity.getMarketSlug().isBlank()) {
                continue;
            }

            try {
                PredictionGammaMarketDto gamma =
                        gammaMarketClient.getMarketBySlug(entity.getMarketSlug());

                if (gamma == null) {
                    failed++;
                    continue;
                }

                updatePriceFields(entity, gamma, now);
                updated++;

                sleepQuietly(50);

            } catch (Exception e) {
                failed++;
                log.warn(
                        "Prediction price refresh failed, marketSlug={}",
                        entity.getMarketSlug(),
                        e
                );
            }
        }

        marketInfoRepository.saveAll(markets);

        log.info(
                "Prediction price refresh finished, total={}, updated={}, failed={}",
                markets.size(),
                updated,
                failed
        );
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 判斷是否需要刷新。
     *
     * 規則：
     * 超過 5 秒才更新。
     */
    private boolean shouldRefresh(
            PredictionMarketInfo entity,
            LocalDateTime now
    ) {
        if (entity.getLastPriceUpdatedAt() == null) {
            return true;
        }

        long seconds =
                Duration.between(
                        entity.getLastPriceUpdatedAt(),
                        now
                ).getSeconds();

        return seconds >= PRICE_REFRESH_SECONDS;
    }

    /**
     * 更新價格欄位。
     *
     * 注意：
     * 不更新：
     * - eventTitle
     * - teamA
     * - teamB
     * - outcomeKey
     * - outcomeLabel
     *
     * 只更新熱資料。
     */
    private void updatePriceFields(
            PredictionMarketInfo entity,
            PredictionGammaMarketDto gamma,
            LocalDateTime now
    ) {
        entity.setBestBid(gamma.getBestBid());

        entity.setBestAsk(gamma.getBestAsk());

        entity.setLastTradePrice(gamma.getLastTradePrice());

        entity.setLiquidity(gamma.getLiquidityNum());

        entity.setVolume(gamma.getVolumeNum());

        entity.setVolume24hr(gamma.getVolume24hr());

        entity.setOutcomePrices(gamma.getOutcomePrices());

        entity.setClobTokenIds(gamma.getClobTokenIds());

        /**
         * 更新 static YES / NO price。
         */
        List<String> prices =
                PredictionJsonUtils.safeStringArray(
                        gamma.getOutcomePrices()
                );

        if (prices.size() >= 2) {
            entity.setStaticYesPrice(
                    PredictionJsonUtils.safeDouble(prices.get(0))
            );

            entity.setStaticNoPrice(
                    PredictionJsonUtils.safeDouble(prices.get(1))
            );
        }

        /**
         * 更新 YES / NO token id。
         */
        List<String> tokenIds =
                PredictionJsonUtils.safeStringArray(
                        gamma.getClobTokenIds()
                );

        if (tokenIds.size() >= 2) {
            entity.setYesTokenId(tokenIds.get(0));

            entity.setNoTokenId(tokenIds.get(1));
        }

        entity.setLastPriceUpdatedAt(now);
    }
}