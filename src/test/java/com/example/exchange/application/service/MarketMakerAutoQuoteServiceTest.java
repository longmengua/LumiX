/*
 * 檔案用途：測試做市商自動報價策略會依 order book top-of-book 產生雙邊 quote。
 */
package com.example.exchange.application.service;

import com.example.exchange.domain.model.dto.MarketMakerAutoQuoteRunReport;
import com.example.exchange.domain.model.dto.MarketMakerProfile;
import com.example.exchange.domain.model.dto.MarketMakerQuoteCommand;
import com.example.exchange.domain.model.dto.MarketMakerRiskLimit;
import com.example.exchange.domain.model.dto.MarketTicker;
import com.example.exchange.domain.model.dto.TopOfBook;
import com.example.exchange.domain.model.dto.SymbolConfig;
import com.example.exchange.domain.repository.MarketMakerProfileStore;
import com.example.exchange.domain.repository.SymbolConfigRepository;
import com.example.exchange.domain.service.MatchingEngine;
import com.example.exchange.infra.config.MarketMakerAutoQuoteProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MarketMakerAutoQuoteServiceTest {

    @Test
    @DisplayName("auto quote 依 top-of-book midpoint、tick 與 minNotional 產生雙邊 quote")
    void runOncePlacesQuoteFromTopOfBookMidpoint() {
        Fixture fixture = new Fixture();
        fixture.profileStore.save(profile(false, "10000"));
        when(fixture.matchingEngine.top("BTCUSDT")).thenReturn(Optional.of(top("100.00", "101.00")));

        // 場景：策略以目前最佳買賣中點 100.5 為基準，至少掛出前台可見的買五檔與賣五檔。
        MarketMakerAutoQuoteRunReport report = fixture.service.runOnce();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<MarketMakerQuoteCommand>> commandCaptor = ArgumentCaptor.forClass(List.class);
        verify(fixture.quoteLifecycleService).placeQuoteLadder(commandCaptor.capture());
        List<MarketMakerQuoteCommand> commands = commandCaptor.getValue();
        MarketMakerQuoteCommand command = commands.getFirst();
        assertThat(report.placedCount()).isEqualTo(1);
        assertThat(report.skippedCount()).isZero();
        assertThat(commands).hasSize(5);
        assertThat(command.marketMakerId()).isEqualTo("mm-auto-1");
        assertThat(command.uid()).isEqualTo(9301);
        assertThat(command.symbol()).isEqualTo("BTCUSDT");
        assertThat(command.bidPrice()).isEqualByComparingTo("100.30");
        assertThat(command.askPrice()).isEqualByComparingTo("100.70");
        assertThat(command.bidQuantity()).isEqualByComparingTo("0.050");
        assertThat(command.askQuantity()).isEqualByComparingTo("0.050");
        assertThat(command.refId()).isEqualTo("auto-test-mm-auto-1-BTCUSDT-1-1");
        assertThat(commands.get(1).bidPrice()).isEqualByComparingTo("100.20");
        assertThat(commands.get(1).askPrice()).isEqualByComparingTo("100.80");
        assertThat(commands).allSatisfy(level ->
                assertThat(level.bidPrice()).isLessThan(level.askPrice()));
        assertThat(commands.get(4).bidPrice()).isEqualByComparingTo("99.90");
        assertThat(commands.get(4).askPrice()).isEqualByComparingTo("101.10");
    }

    @Test
    @DisplayName("auto quote 優先用 ticker fair value 與費用 buffer 做安全報價")
    void runOnceUsesTickerFairValueAndFeeBufferBeforeStrategyLadder() {
        Fixture fixture = new Fixture();
        fixture.profileStore.save(profile(false, "10000"));
        fixture.properties.setHalfSpreadTicks(1);
        fixture.properties.setHedgeCostRate(new BigDecimal("0.0030"));
        when(fixture.matchingEngine.top("BTCUSDT")).thenReturn(Optional.of(top("90.00", "110.00")));
        when(fixture.marketDataService.ticker("BTCUSDT")).thenReturn(Optional.of(new MarketTicker(
                "BTCUSDT",
                new BigDecimal("100.00"),
                new BigDecimal("99.90"),
                new BigDecimal("100.10"),
                BigDecimal.ZERO,
                null,
                null,
                Instant.now()
        )));

        // 場景：安全做市先用外部/market-data fair mid 100，再加 maker+hedge 成本，不能只貼著內部簿掛單。
        fixture.service.runOnce();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<MarketMakerQuoteCommand>> commandCaptor = ArgumentCaptor.forClass(List.class);
        verify(fixture.quoteLifecycleService).placeQuoteLadder(commandCaptor.capture());
        MarketMakerQuoteCommand nearest = commandCaptor.getValue().getFirst();
        assertThat(nearest.bidPrice()).isEqualByComparingTo("99.60");
        assertThat(nearest.askPrice()).isEqualByComparingTo("100.40");
    }

    @Test
    @DisplayName("auto quote 遇到 kill switch 時不送 quote lifecycle")
    void runOnceSkipsKillSwitchProfileSymbol() {
        Fixture fixture = new Fixture();
        fixture.profileStore.save(profile(true, "10000"));
        when(fixture.matchingEngine.top("BTCUSDT")).thenReturn(Optional.of(top("100.00", "101.00")));

        // 場景：risk limit kill switch 生效時，自動策略只能留下 skipped report，不能改動 order book。
        MarketMakerAutoQuoteRunReport report = fixture.service.runOnce();

        assertThat(report.placedCount()).isZero();
        assertThat(report.skippedCount()).isEqualTo(1);
        assertThat(report.results().getFirst().reason()).isEqualTo("KILL_SWITCH_ENABLED");
        verify(fixture.quoteLifecycleService, never()).placeQuoteLadder(any());
    }

    @Test
    @DisplayName("auto quote 若 risk limit 無法同時滿足 minNotional 會跳過")
    void runOnceSkipsWhenRiskLimitIsBelowMinimumNotional() {
        Fixture fixture = new Fixture();
        fixture.profileStore.save(profile(false, "4.00"));
        when(fixture.matchingEngine.top("BTCUSDT")).thenReturn(Optional.of(top("100.00", "101.00")));

        // 場景：symbol minNotional 是 5，但做市商單筆上限只有 4，策略不得硬送無法通過風控的 quote。
        MarketMakerAutoQuoteRunReport report = fixture.service.runOnce();

        assertThat(report.placedCount()).isZero();
        assertThat(report.skippedCount()).isEqualTo(1);
        assertThat(report.results().getFirst().reason()).isEqualTo("QUOTE_SIZE_BELOW_MIN_NOTIONAL_OR_RISK_LIMIT");
        verify(fixture.quoteLifecycleService, never()).placeQuoteLadder(any());
    }

    @Test
    @DisplayName("auto quote 遇到單邊 top-of-book 時用可見價格補出雙邊 quote")
    void runOnceQuotesFromSingleSidedTopOfBook() {
        Fixture fixture = new Fixture();
        fixture.profileStore.save(profile(false, "10000"));
        when(fixture.matchingEngine.top("BTCUSDT")).thenReturn(Optional.of(TopOfBook.builder()
                .bestBid(new BigDecimal("100.00"))
                .build()));

        // 場景：只有 bid 時，做市商仍要補出 ask；post-only 邊界確保 quote 不吃掉可見 top。
        MarketMakerAutoQuoteRunReport report = fixture.service.runOnce();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<MarketMakerQuoteCommand>> commandCaptor = ArgumentCaptor.forClass(List.class);
        verify(fixture.quoteLifecycleService).placeQuoteLadder(commandCaptor.capture());
        MarketMakerQuoteCommand command = commandCaptor.getValue().getFirst();
        assertThat(report.placedCount()).isEqualTo(1);
        assertThat(command.bidPrice()).isEqualByComparingTo("99.80");
        assertThat(command.askPrice()).isEqualByComparingTo("100.20");
    }

    private static MarketMakerProfile profile(boolean killSwitch, String maxOrderNotional) {
        return new MarketMakerProfile(
                "mm-auto-1",
                9301,
                true,
                List.of(new MarketMakerRiskLimit(
                        "BTCUSDT",
                        new BigDecimal("1000000"),
                        new BigDecimal("1000000"),
                        new BigDecimal(maxOrderNotional),
                        new BigDecimal("0.01"),
                        killSwitch
                ))
        );
    }

    private static TopOfBook top(String bid, String ask) {
        return TopOfBook.builder()
                .bestBid(new BigDecimal(bid))
                .bestAsk(new BigDecimal(ask))
                .build();
    }

    private static SymbolConfig symbolConfig() {
        return SymbolConfig.builder()
                .symbol("BTCUSDT")
                .baseAsset("BTC")
                .quoteAsset("USDT")
                .priceTick(new BigDecimal("0.10"))
                .lotSize(new BigDecimal("0.001"))
                .minQty(new BigDecimal("0.001"))
                .minNotional(new BigDecimal("5.00"))
                .maxOrderNotional(new BigDecimal("10000"))
                .tradingEnabled(true)
                .build();
    }

    private static final class Fixture {
        private final MemProfileStore profileStore = new MemProfileStore();
        private final MarketMakerQuoteLifecycleService quoteLifecycleService = mock(MarketMakerQuoteLifecycleService.class);
        private final MatchingEngine matchingEngine = mock(MatchingEngine.class);
        private final MarketDataService marketDataService = mock(MarketDataService.class);
        private final SymbolConfigRepository symbolConfigRepository = symbol -> Optional.of(symbolConfig());
        private final MarketMakerAutoQuoteProperties properties = properties();
        private final MarketMakerAutoQuoteService service = new MarketMakerAutoQuoteService(
                new MarketMakerProfileService(profileStore),
                quoteLifecycleService,
                matchingEngine,
                symbolConfigRepository,
                marketDataService,
                properties
        );

        private Fixture() {
            when(marketDataService.ticker("BTCUSDT")).thenReturn(Optional.empty());
        }

        private static MarketMakerAutoQuoteProperties properties() {
            MarketMakerAutoQuoteProperties properties = new MarketMakerAutoQuoteProperties();
            properties.setQuoteQuantity(new BigDecimal("0.010"));
            properties.setHalfSpreadTicks(2);
            properties.setLadderLevelsPerSide(5);
            properties.setPulseTicks(0);
            properties.setRefPrefix("auto-test");
            return properties;
        }
    }

    private static final class MemProfileStore implements MarketMakerProfileStore {
        private final Map<String, MarketMakerProfile> profiles = new LinkedHashMap<>();

        @Override
        public void save(MarketMakerProfile profile) {
            profiles.put(profile.marketMakerId(), profile);
        }

        @Override
        public Optional<MarketMakerProfile> findByMarketMakerId(String marketMakerId) {
            return Optional.ofNullable(profiles.get(marketMakerId));
        }

        @Override
        public Optional<MarketMakerProfile> findByUid(long uid) {
            return profiles.values().stream()
                    .filter(profile -> profile.uid() == uid)
                    .findFirst();
        }

        @Override
        public List<MarketMakerProfile> findAll() {
            // Admin profile lists must include disabled profiles so operators can re-enable them.
            return List.copyOf(profiles.values());
        }

        @Override
        public List<MarketMakerProfile> findEnabled() {
            return profiles.values().stream()
                    .filter(MarketMakerProfile::enabled)
                    .toList();
        }
    }
}
