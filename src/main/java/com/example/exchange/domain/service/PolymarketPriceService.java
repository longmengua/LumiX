package com.example.exchange.domain.service;

import com.example.exchange.domain.model.dto.PredictionGammaMarketDto;
import com.example.exchange.domain.model.dto.PredictionPriceRefreshResult;
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
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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
     * 2. Gamma 拉 FIFA World Cup events 並 flatten markets
     * 3. 用 marketSlug match
     * 4. 更新價格欄位
     */
    @Transactional
    public PredictionPriceRefreshResult refreshPrices() {
        return refreshPrices(false);
    }

    /**
     * 手動刷新價格。
     *
     * Controller 入口會使用 forceRefresh=true，方便人工測試時立即打 Gamma。
     * Scheduler 入口使用預設 refreshPrices()，避免同一批資料被高頻重複刷新。
     */
    @Transactional
    public PredictionPriceRefreshResult refreshPrices(boolean forceRefresh) {
        LocalDateTime now = LocalDateTime.now();

        List<PredictionMarketInfo> markets =
                marketInfoRepository.findByActiveTrueAndClosedFalseOrderByEventDateAsc();

        if (markets.isEmpty()) {
            log.info("Prediction price refresh skipped, no active markets");
            return PredictionPriceRefreshResult.builder()
                    .totalCount(0)
                    .updatedCount(0)
                    .skippedCount(0)
                    .failedCount(0)
                    .forceRefresh(forceRefresh)
                    .message("no active markets")
                    .build();
        }

        Map<String, PredictionGammaMarketDto> gammaBySlug =
                gammaMarketClient.fetchFifaWorldCupMarkets()
                        .stream()
                        .filter(gamma -> gamma.getSlug() != null && !gamma.getSlug().isBlank())
                        .collect(Collectors.toMap(
                                PredictionGammaMarketDto::getSlug,
                                Function.identity(),
                                this::pickMoreLiquidMarket
                        ));

        if (gammaBySlug.isEmpty()) {
            log.warn("Prediction price refresh failed, Gamma FIFA markets empty");
            return PredictionPriceRefreshResult.builder()
                    .totalCount(markets.size())
                    .updatedCount(0)
                    .skippedCount(0)
                    .failedCount(markets.size())
                    .forceRefresh(forceRefresh)
                    .message("Gamma FIFA markets empty")
                    .build();
        }

        int updated = 0;
        int skipped = 0;
        int failed = 0;

        for (PredictionMarketInfo entity : markets) {
            if (entity.getMarketSlug() == null || entity.getMarketSlug().isBlank()) {
                skipped++;
                continue;
            }

            if (!forceRefresh && !shouldRefresh(entity, now)) {
                skipped++;
                continue;
            }

            try {
                PredictionGammaMarketDto gamma =
                        gammaBySlug.get(entity.getMarketSlug());

                if (gamma == null) {
                    failed++;
                    continue;
                }

                updatePriceFields(entity, gamma, now);
                updated++;

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
                "Prediction price refresh finished, total={}, updated={}, skipped={}, failed={}, force={}",
                markets.size(),
                updated,
                skipped,
                failed,
                forceRefresh
        );

        return PredictionPriceRefreshResult.builder()
                .totalCount(markets.size())
                .updatedCount(updated)
                .skippedCount(skipped)
                .failedCount(failed)
                .forceRefresh(forceRefresh)
                .message("Prediction market price refresh finished")
                .build();
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

        entity.setNegRisk(Boolean.TRUE.equals(gamma.getNegRisk()));

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

        refreshNoPrices(entity);

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

    private void refreshNoPrices(PredictionMarketInfo entity) {
        if (entity.getStaticNoPrice() != null && entity.getStaticNoPrice() > 0) {
            entity.setNoBuyPrice(entity.getStaticNoPrice());
        } else if (entity.getBestBid() != null) {
            entity.setNoBuyPrice(1D - entity.getBestBid());
        }

        if (entity.getStaticNoPrice() != null && entity.getStaticNoPrice() > 0) {
            entity.setNoSellPrice(entity.getStaticNoPrice());
        } else if (entity.getBestAsk() != null) {
            entity.setNoSellPrice(1D - entity.getBestAsk());
        }
    }

    private PredictionGammaMarketDto pickMoreLiquidMarket(
            PredictionGammaMarketDto first,
            PredictionGammaMarketDto second
    ) {
        Double firstLiquidity = first.getLiquidityNum();
        Double secondLiquidity = second.getLiquidityNum();

        double a = firstLiquidity == null ? 0D : firstLiquidity;
        double b = secondLiquidity == null ? 0D : secondLiquidity;

        return b > a ? second : first;
    }
}
