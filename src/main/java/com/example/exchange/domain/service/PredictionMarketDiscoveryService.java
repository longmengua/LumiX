package com.example.exchange.domain.service;

import com.example.exchange.domain.model.dto.PredictionGammaEventDto;
import com.example.exchange.domain.model.dto.PredictionGammaMarketDto;
import com.example.exchange.domain.model.entity.PredictionMarketInfoEntity;
import com.example.exchange.domain.model.entity.PredictionMarketSyncKeyEntity;
import com.example.exchange.domain.repository.client.PredictionGammaMarketClient;
import com.example.exchange.domain.repository.jpa.PredictionMarketInfoRepository;
import com.example.exchange.domain.repository.jpa.PredictionMarketSyncKeyRepository;
import com.example.exchange.domain.util.PredictionJsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Prediction Market Discovery Service。
 *
 * 用途：
 * 手動全量拉 Gamma markets，
 * 自動發現 FIFA World Cup markets，
 * 然後建立：
 *
 * 1. prediction_market_sync_key
 * 2. prediction_market_info
 *
 * 注意：
 * 這個服務是「重任務」。
 * 不建議排程每 5 秒跑。
 *
 * 建議只透過手動 API 觸發：
 * POST /api/prediction/markets/discover
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PredictionMarketDiscoveryService {

    private static final String SOURCE = "FIFA_2026";
    private static final String SERIES_FIFA_WORLD_CUP = "soccer-fifwc";

    private static final String OUTCOME_HOME_WIN = "homeWin";
    private static final String OUTCOME_DRAW = "draw";
    private static final String OUTCOME_AWAY_WIN = "awayWin";

    private final PredictionGammaMarketClient gammaMarketClient;
    private final PredictionMarketSyncKeyRepository syncKeyRepository;
    private final PredictionMarketInfoRepository marketInfoRepository;

    /**
     * 全量 discovery。
     *
     * 流程：
     * 1. Gamma 全量拉 active/open markets
     * 2. 過濾 FIFA World Cup
     * 3. 按 eventSlug group
     * 4. 自動建立 sync key
     * 5. 自動建立 outcome market info
     */
    @Transactional
    public String discoverWorldCupMarkets() {
        List<PredictionGammaMarketDto> allMarkets =
                gammaMarketClient.fetchAllActiveMarkets();

        List<PredictionGammaMarketDto> fifaMarkets =
                allMarkets.stream()
                        .filter(this::isFifaWorldCupMarket)
                        .toList();

        Map<String, List<PredictionGammaMarketDto>> marketsByEventSlug =
                groupByEventSlug(fifaMarkets);

        int eventCount = 0;
        int outcomeCount = 0;

        for (Map.Entry<String, List<PredictionGammaMarketDto>> entry : marketsByEventSlug.entrySet()) {
            String eventSlug = entry.getKey();
            List<PredictionGammaMarketDto> eventMarkets = entry.getValue();

            PredictionGammaEventDto event = firstEvent(eventMarkets.get(0));

            if (event == null) {
                continue;
            }

            PredictionMarketSyncKeyEntity key =
                    upsertSyncKey(eventSlug, event, eventMarkets);

            Map<String, PredictionGammaMarketDto> bestByOutcome =
                    classifyAndPickBest(key, eventMarkets);

            for (Map.Entry<String, PredictionGammaMarketDto> outcomeEntry : bestByOutcome.entrySet()) {
                saveMarketInfo(
                        key,
                        outcomeEntry.getKey(),
                        outcomeEntry.getValue()
                );
                outcomeCount++;
            }

            key.setSyncStatus("SUCCESS");
            key.setLastError(null);
            key.setLastSyncedAt(LocalDateTime.now());
            syncKeyRepository.save(key);

            eventCount++;
        }

        return "discover finished, events=" + eventCount + ", outcomes=" + outcomeCount;
    }

    /**
     * 判斷是否 FIFA World Cup market。
     *
     * 對齊 TS：
     * - event.seriesSlug == soccer-fifwc
     * - event.slug startsWith fifwc-
     * - market.slug startsWith fifwc-
     */
    private boolean isFifaWorldCupMarket(PredictionGammaMarketDto market) {
        if (startsWithFifwc(market.getSlug())) {
            return true;
        }

        if (market.getEvents() == null) {
            return false;
        }

        return market.getEvents()
                .stream()
                .anyMatch(event ->
                        SERIES_FIFA_WORLD_CUP.equals(event.getSeriesSlug())
                                || startsWithFifwc(event.getSlug())
                );
    }

    /**
     * eventSlug -> markets。
     */
    private Map<String, List<PredictionGammaMarketDto>> groupByEventSlug(
            List<PredictionGammaMarketDto> markets
    ) {
        Map<String, List<PredictionGammaMarketDto>> result = new HashMap<>();

        for (PredictionGammaMarketDto market : markets) {
            PredictionGammaEventDto event = firstEvent(market);

            if (event == null || event.getSlug() == null || event.getSlug().isBlank()) {
                continue;
            }

            result.computeIfAbsent(event.getSlug(), k -> new ArrayList<>())
                    .add(market);
        }

        return result;
    }

    /**
     * 建立或更新 sync key。
     *
     * 注意：
     * discovery 模式下，不需要人工 insert sync key。
     */
    private PredictionMarketSyncKeyEntity upsertSyncKey(
            String eventSlug,
            PredictionGammaEventDto event,
            List<PredictionGammaMarketDto> eventMarkets
    ) {
        PredictionMarketSyncKeyEntity key =
                syncKeyRepository.findByEventSlug(eventSlug)
                        .orElseGet(PredictionMarketSyncKeyEntity::new);

        String eventTitle = event.getTitle();

        TeamPair teamPair = parseTeamPair(eventTitle);

        key.setEventSlug(eventSlug);
        key.setEventTitle(eventTitle);
        key.setTeamA(teamPair.teamA());
        key.setTeamB(teamPair.teamB());
        key.setEventDate(resolveEventDate(event, eventMarkets));
        key.setSource(SOURCE);
        key.setSyncEnabled(true);

        if (key.getSyncStatus() == null) {
            key.setSyncStatus("PENDING");
        }

        if (key.getRetryCount() == null) {
            key.setRetryCount(0);
        }

        return syncKeyRepository.save(key);
    }

    /**
     * 從 event title 解析 teamA / teamB。
     *
     * 常見格式：
     * Mexico vs South Africa
     */
    private TeamPair parseTeamPair(String title) {
        if (title == null || title.isBlank()) {
            return new TeamPair("UNKNOWN", "UNKNOWN");
        }

        String normalized = title.replace(" v ", " vs ");

        String[] parts = normalized.split("(?i)\\s+vs\\s+");

        if (parts.length >= 2) {
            return new TeamPair(
                    parts[0].trim(),
                    parts[1].trim()
            );
        }

        return new TeamPair(title.trim(), "UNKNOWN");
    }

    /**
     * 解析 event date。
     *
     * 優先順序：
     * 1. event.eventDate
     * 2. event.startTime
     * 3. market.endDate
     * 4. fallback now date
     */
    private LocalDate resolveEventDate(
            PredictionGammaEventDto event,
            List<PredictionGammaMarketDto> eventMarkets
    ) {
        LocalDate date = parseDate(event.getEventDate());

        if (date != null) {
            return date;
        }

        date = parseDate(event.getStartTime());

        if (date != null) {
            return date;
        }

        for (PredictionGammaMarketDto market : eventMarkets) {
            date = parseDate(market.getEndDate());

            if (date != null) {
                return date;
            }
        }

        return LocalDate.now();
    }

    /**
     * 分類 homeWin / draw / awayWin，
     * 同一 outcome 取 liquidity 最大的 market。
     */
    private Map<String, PredictionGammaMarketDto> classifyAndPickBest(
            PredictionMarketSyncKeyEntity key,
            List<PredictionGammaMarketDto> markets
    ) {
        Map<String, PredictionGammaMarketDto> result = new HashMap<>();

        for (PredictionGammaMarketDto market : markets) {
            String outcomeKey = classifyOutcome(key, market);

            if (outcomeKey == null) {
                continue;
            }

            PredictionGammaMarketDto old = result.get(outcomeKey);

            if (old == null || liquidity(market) > liquidity(old)) {
                result.put(outcomeKey, market);
            }
        }

        return result;
    }

    /**
     * outcome 分類。
     */
    private String classifyOutcome(
            PredictionMarketSyncKeyEntity key,
            PredictionGammaMarketDto market
    ) {
        String text = norm(
                safe(market.getGroupItemTitle())
                        + " "
                        + safe(market.getQuestion())
                        + " "
                        + safe(market.getSlug())
        );

        if (text.contains("draw")) {
            return OUTCOME_DRAW;
        }

        String home = norm(key.getTeamA());
        String away = norm(key.getTeamB());

        boolean hasHome = !home.isBlank() && text.contains(home);
        boolean hasAway = !away.isBlank() && text.contains(away);

        if (hasHome && !hasAway) {
            return OUTCOME_HOME_WIN;
        }

        if (hasAway && !hasHome) {
            return OUTCOME_AWAY_WIN;
        }

        return null;
    }

    /**
     * 寫入 prediction_market_info。
     */
    private void saveMarketInfo(
            PredictionMarketSyncKeyEntity key,
            String outcomeKey,
            PredictionGammaMarketDto market
    ) {
        PredictionMarketInfoEntity entity =
                marketInfoRepository.findByMarketSlug(market.getSlug())
                        .orElseGet(PredictionMarketInfoEntity::new);

        entity.setEventSlug(key.getEventSlug());
        entity.setEventTitle(key.getEventTitle());
        entity.setTeamA(key.getTeamA());
        entity.setTeamB(key.getTeamB());
        entity.setEventDate(key.getEventDate());

        entity.setConditionId(
                market.getConditionId() != null
                        ? market.getConditionId()
                        : market.getId()
        );

        entity.setQuestion(market.getQuestion());
        entity.setMarketSlug(market.getSlug());

        entity.setOutcomeKey(outcomeKey);
        entity.setOutcomeLabel(toOutcomeLabel(key, outcomeKey));

        entity.setActive(Boolean.TRUE.equals(market.getActive()));
        entity.setClosed(Boolean.TRUE.equals(market.getClosed()));
        entity.setAcceptingOrders(Boolean.TRUE.equals(market.getAcceptingOrders()));
        entity.setEnableOrderBook(Boolean.TRUE.equals(market.getEnableOrderBook()));

        entity.setBestBid(market.getBestBid());
        entity.setBestAsk(market.getBestAsk());
        entity.setLastTradePrice(market.getLastTradePrice());

        entity.setLiquidity(market.getLiquidityNum());
        entity.setVolume(market.getVolumeNum());
        entity.setVolume24hr(market.getVolume24hr());

        entity.setOutcomePrices(market.getOutcomePrices());
        entity.setClobTokenIds(market.getClobTokenIds());

        List<String> prices =
                PredictionJsonUtils.safeStringArray(market.getOutcomePrices());

        if (prices.size() >= 2) {
            entity.setStaticYesPrice(PredictionJsonUtils.safeDouble(prices.get(0)));
            entity.setStaticNoPrice(PredictionJsonUtils.safeDouble(prices.get(1)));
        }

        List<String> tokenIds =
                PredictionJsonUtils.safeStringArray(market.getClobTokenIds());

        if (tokenIds.size() >= 2) {
            entity.setYesTokenId(tokenIds.get(0));
            entity.setNoTokenId(tokenIds.get(1));
        }

        entity.setLastPriceUpdatedAt(LocalDateTime.now());

        marketInfoRepository.save(entity);
    }

    private String toOutcomeLabel(
            PredictionMarketSyncKeyEntity key,
            String outcomeKey
    ) {
        return switch (outcomeKey) {
            case OUTCOME_HOME_WIN -> key.getTeamA();
            case OUTCOME_DRAW -> "Draw";
            case OUTCOME_AWAY_WIN -> key.getTeamB();
            default -> outcomeKey;
        };
    }

    private PredictionGammaEventDto firstEvent(PredictionGammaMarketDto market) {
        if (market.getEvents() == null || market.getEvents().isEmpty()) {
            return null;
        }

        return market.getEvents().get(0);
    }

    private boolean startsWithFifwc(String value) {
        return value != null && norm(value).startsWith("fifwc-");
    }

    private double liquidity(PredictionGammaMarketDto market) {
        return market.getLiquidityNum() == null ? 0D : market.getLiquidityNum();
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return LocalDate.parse(value.substring(0, 10));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 對齊 TS norm()。
     */
    private String norm(String value) {
        if (value == null) {
            return "";
        }

        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");

        return normalized
                .toLowerCase(Locale.ROOT)
                .replace("&", "and")
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private record TeamPair(
            String teamA,
            String teamB
    ) {
    }
}