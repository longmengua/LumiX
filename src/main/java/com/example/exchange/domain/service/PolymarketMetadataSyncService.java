package com.example.exchange.domain.service;

import com.example.exchange.domain.model.dto.PredictionGammaEventDto;
import com.example.exchange.domain.model.dto.PredictionGammaMarketDto;
import com.example.exchange.domain.model.entity.PredictionMarketInfoEntity;
import com.example.exchange.domain.model.entity.PredictionMarketSyncKeyEntity;
import com.example.exchange.domain.repository.jpa.PredictionMarketInfoRepository;
import com.example.exchange.domain.util.PredictionJsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Prediction Market Metadata Sync Service。
 *
 * 職責：
 * 1. 根據已知 sync key 找 Gamma event markets
 * 2. 分類 homeWin / draw / awayWin
 * 3. 同一 outcome 選 liquidity 最大的 market
 * 4. 寫入 prediction_market_info
 *
 * 注意：
 * 這個 Service 不主動呼叫 Gamma API。
 * Gamma API 由 FullSyncService / DiscoveryService 呼叫後，把 allMarkets 傳進來。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PolymarketMetadataSyncService {

    private static final String SERIES_FIFA_WORLD_CUP = "soccer-fifwc";

    private static final String OUTCOME_HOME_WIN = "homeWin";
    private static final String OUTCOME_DRAW = "draw";
    private static final String OUTCOME_AWAY_WIN = "awayWin";

    private final PredictionMarketInfoRepository marketInfoRepository;

    /**
     * 同步單一 sync key。
     *
     * @param key       已知世界杯 fixture / event
     * @param allMarkets Gamma active/open markets
     * @return 成功寫入 outcome 數量
     */
    @Transactional
    public int syncOneKey(
            PredictionMarketSyncKeyEntity key,
            List<PredictionGammaMarketDto> allMarkets
    ) {
        List<PredictionGammaMarketDto> relatedMarkets =
                allMarkets.stream()
                        .filter(this::isFifaWorldCupMarket)
                        .filter(market -> eventMatchesKey(market, key))
                        .toList();

        if (relatedMarkets.isEmpty()) {
            log.warn(
                    "No related markets found, eventSlug={}, teamA={}, teamB={}, eventDate={}",
                    key.getEventSlug(),
                    key.getTeamA(),
                    key.getTeamB(),
                    key.getEventDate()
            );
            return 0;
        }

        Map<String, PredictionGammaMarketDto> bestByOutcome =
                classifyAndPickBest(key, relatedMarkets);

        int saved = 0;

        for (Map.Entry<String, PredictionGammaMarketDto> entry : bestByOutcome.entrySet()) {
            saveMarketInfo(key, entry.getKey(), entry.getValue());
            saved++;
        }

        log.info(
                "Prediction metadata synced, eventSlug={}, related={}, saved={}",
                key.getEventSlug(),
                relatedMarkets.size(),
                saved
        );

        return saved;
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
     * 判斷 Gamma market 是否對應目前 sync key。
     *
     * 優先：
     * 1. eventSlug 完全一致
     *
     * fallback：
     * 2. eventSlug / eventTitle 同時包含 teamA、teamB、eventDate
     */
    private boolean eventMatchesKey(
            PredictionGammaMarketDto market,
            PredictionMarketSyncKeyEntity key
    ) {
        PredictionGammaEventDto event = firstEvent(market);

        if (event == null) {
            return false;
        }

        String gammaEventSlugRaw = safe(event.getSlug());
        String keyEventSlugRaw = safe(key.getEventSlug());

        /**
         * 如果 sync key 已經有 eventSlug，優先精準匹配。
         */
        if (!keyEventSlugRaw.isBlank()
                && gammaEventSlugRaw.equalsIgnoreCase(keyEventSlugRaw)) {
            return true;
        }

        String gammaEventSlug = norm(event.getSlug());
        String gammaEventTitle = norm(event.getTitle());

        List<String> teamATokens = teamTokens(key.getTeamA());
        List<String> teamBTokens = teamTokens(key.getTeamB());

        String date = key.getEventDate() == null
                ? ""
                : key.getEventDate().toString();

        boolean slugHasDate = !date.isBlank() && gammaEventSlug.contains(date);
        boolean slugHasA = containsAny(gammaEventSlug, teamATokens);
        boolean slugHasB = containsAny(gammaEventSlug, teamBTokens);

        boolean titleHasA = containsAny(gammaEventTitle, teamATokens);
        boolean titleHasB = containsAny(gammaEventTitle, teamBTokens);

        return (slugHasDate && slugHasA && slugHasB)
                || (titleHasA && titleHasB);
    }

    /**
     * 分類並選出每個 outcome liquidity 最大的 market。
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
     * 判斷 outcome。
     *
     * 對齊 TS classifyOutcome：
     * text = groupItemTitle + question + slug
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

        List<String> homeTokens = teamTokens(key.getTeamA());
        List<String> awayTokens = teamTokens(key.getTeamB());

        boolean hasHome = containsAny(text, homeTokens);
        boolean hasAway = containsAny(text, awayTokens);

        if (hasHome && !hasAway) {
            return OUTCOME_HOME_WIN;
        }

        if (hasAway && !hasHome) {
            return OUTCOME_AWAY_WIN;
        }

        String group = norm(market.getGroupItemTitle());

        if (equalsOrStartsWith(group, homeTokens)) {
            return OUTCOME_HOME_WIN;
        }

        if (equalsOrStartsWith(group, awayTokens)) {
            return OUTCOME_AWAY_WIN;
        }

        return null;
    }

    /**
     * 寫入 / 更新 prediction_market_info。
     */
    private void saveMarketInfo(
            PredictionMarketSyncKeyEntity key,
            String outcomeKey,
            PredictionGammaMarketDto market
    ) {
        PredictionMarketInfoEntity entity =
                marketInfoRepository.findByMarketSlug(market.getSlug())
                        .orElseGet(PredictionMarketInfoEntity::new);

        PredictionGammaEventDto event = firstEvent(market);

        entity.setEventSlug(
                key.getEventSlug() == null || key.getEventSlug().isBlank()
                        ? event == null ? null : event.getSlug()
                        : key.getEventSlug()
        );

        entity.setEventTitle(
                event == null || event.getTitle() == null
                        ? key.getEventTitle()
                        : event.getTitle()
        );

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

    private boolean containsAny(String text, List<String> tokens) {
        for (String token : tokens) {
            if (!token.isBlank() && text.contains(token)) {
                return true;
            }
        }

        return false;
    }

    private boolean equalsOrStartsWith(String text, List<String> tokens) {
        for (String token : tokens) {
            if (text.equals(token) || text.startsWith(token + "-")) {
                return true;
            }
        }

        return false;
    }

    /**
     * team name + team code tokens。
     */
    private List<String> teamTokens(String team) {
        List<String> tokens = new ArrayList<>();

        tokens.add(norm(team));

        for (String code : teamCodes(team)) {
            tokens.add(norm(code));
        }

        return tokens.stream()
                .filter(x -> !x.isBlank())
                .distinct()
                .toList();
    }

    /**
     * 對齊 TS TEAM_CODE。
     */
    private List<String> teamCodes(String team) {
        if (team == null) {
            return List.of();
        }

        return switch (team) {
            case "Mexico" -> List.of("mex");
            case "South Africa" -> List.of("rsa", "south-africa");
            case "South Korea" -> List.of("kor", "south-korea");
            case "Czechia" -> List.of("cze", "czechia", "czech-republic");
            case "Canada" -> List.of("can");
            case "Bosnia and Herzegovina" -> List.of("bih", "bosnia");
            case "USA" -> List.of("usa", "united-states");
            case "Paraguay" -> List.of("par");
            case "Qatar" -> List.of("qat");
            case "Switzerland" -> List.of("sui", "switzerland");
            case "Australia" -> List.of("aus");
            case "Türkiye" -> List.of("tur", "turkiye", "turkey");
            case "Brazil" -> List.of("bra");
            case "Morocco" -> List.of("mar");
            case "Haiti" -> List.of("hai");
            case "Scotland" -> List.of("sco");
            case "Germany" -> List.of("ger");
            case "Curacao" -> List.of("cur", "curacao");
            case "Ivory Coast" -> List.of("civ", "ivory-coast", "cote-divoire");
            case "Ecuador" -> List.of("ecu");
            case "Netherlands" -> List.of("ned", "netherlands", "holland");
            case "Japan" -> List.of("jpn", "japan");
            case "Sweden" -> List.of("swe");
            case "Tunisia" -> List.of("tun");
            case "Poland" -> List.of("pol");
            case "Senegal" -> List.of("sen");
            case "Saudi Arabia" -> List.of("ksa", "sau", "saudi-arabia");
            case "Uruguay" -> List.of("uru");
            case "Spain" -> List.of("esp", "spain");
            case "Cape Verde" -> List.of("cpv", "cape-verde");
            case "Belgium" -> List.of("bel");
            case "Egypt" -> List.of("egy");
            case "Iran" -> List.of("irn", "iran");
            case "New Zealand" -> List.of("nzl", "new-zealand");
            case "France" -> List.of("fra");
            case "Norway" -> List.of("nor");
            case "Iraq" -> List.of("irq", "iraq");
            case "Argentina" -> List.of("arg");
            case "Algeria" -> List.of("alg");
            case "Austria" -> List.of("aut");
            case "Jordan" -> List.of("jor");
            case "Portugal" -> List.of("por");
            case "Colombia" -> List.of("col");
            case "Uzbekistan" -> List.of("uzb");
            case "England" -> List.of("eng");
            case "Croatia" -> List.of("cro");
            case "Ghana" -> List.of("gha");
            case "Panama" -> List.of("pan");
            default -> List.of(norm(team));
        };
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
}