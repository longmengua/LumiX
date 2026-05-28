/*
 * 檔案用途：測試做市商 hedge strategy execution orchestration。
 */
package com.example.exchange.application.service;

import com.example.exchange.domain.event.HedgeDecisionRecorded;
import com.example.exchange.domain.model.dto.HedgeDecision;
import com.example.exchange.domain.model.dto.HedgeExecutionReport;
import com.example.exchange.domain.model.dto.HedgeOrderRequest;
import com.example.exchange.domain.model.dto.HedgeOrderResult;
import com.example.exchange.domain.model.dto.MarketMakerProfile;
import com.example.exchange.domain.model.dto.MarketMakerRiskLimit;
import com.example.exchange.domain.model.entity.Position;
import com.example.exchange.domain.model.entity.Symbol;
import com.example.exchange.domain.model.enums.OrderSide;
import com.example.exchange.domain.repository.MarketMakerProfileStore;
import com.example.exchange.domain.repository.PositionRepository;
import com.example.exchange.domain.service.HedgeVenueAdapter;
import com.example.exchange.infra.config.MarkPriceOracleProperties;
import com.example.exchange.infra.config.RiskControlsProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class MarketMakerHedgeExecutionServiceTest {

    @Test
    @DisplayName("executeForMarketMaker 只會把超過 inventory limit 的 exposure 送往 hedge venue")
    void executeForMarketMakerRoutesOnlyPlannedHedges() {
        Fixture fixture = new Fixture();
        fixture.profileStore.save(profile("mm-1", 9001, true, false));
        fixture.addPosition(9001, "BTCUSDT", "2.000");
        fixture.oracle.update("BTCUSDT", new BigDecimal("100.00"), new BigDecimal("100.00"), "test");

        // 流程：long notional=200 超過 limit=150，execution 應產生一筆 reduce-only SELL hedge。
        HedgeExecutionReport report = fixture.service.executeForMarketMaker("mm-1", "exec-test");

        assertThat(report.exposureCount()).isEqualTo(1);
        assertThat(report.plannedCount()).isEqualTo(1);
        assertThat(report.routedCount()).isEqualTo(1);
        assertThat(report.hedgeDecisions()).extracting(HedgeDecision::accepted).containsExactly(true);
        assertThat(fixture.venue.requests).extracting(HedgeOrderRequest::side).containsExactly(OrderSide.SELL);
        assertThat(fixture.published).hasSize(1);
    }

    @Test
    @DisplayName("executeForEnabledMarketMakers 會跳過未啟用 profile 且不對未超限 exposure 下單")
    void executeForEnabledMarketMakersSkipsDisabledAndWithinLimitProfiles() {
        Fixture fixture = new Fixture();
        fixture.profileStore.save(profile("mm-enabled", 9001, true, false));
        fixture.profileStore.save(profile("mm-disabled", 9002, false, false));
        fixture.addPosition(9001, "BTCUSDT", "1.000");
        fixture.addPosition(9002, "BTCUSDT", "3.000");
        fixture.oracle.update("BTCUSDT", new BigDecimal("100.00"), new BigDecimal("100.00"), "test");

        // 流程：只取 enabled profile；enabled profile 未超限，所以不會送 venue。
        List<HedgeExecutionReport> reports = fixture.service.executeForEnabledMarketMakers("exec-test");

        assertThat(reports).hasSize(1);
        assertThat(reports.getFirst().marketMakerId()).isEqualTo("mm-enabled");
        assertThat(reports.getFirst().plannedCount()).isZero();
        assertThat(fixture.venue.requests).isEmpty();
    }

    @Test
    @DisplayName("executeForMarketMaker 遇到 kill switch strategy decision 時不送 hedge venue")
    void executeForMarketMakerDoesNotRouteWhenKillSwitchIsEnabled() {
        Fixture fixture = new Fixture();
        fixture.profileStore.save(profile("mm-1", 9001, true, true));
        fixture.addPosition(9001, "BTCUSDT", "2.000");
        fixture.oracle.update("BTCUSDT", new BigDecimal("100.00"), new BigDecimal("100.00"), "test");

        // 流程：即使 exposure 超限，只要 risk limit kill switch 開啟，就只能回報 strategy rejection。
        HedgeExecutionReport report = fixture.service.executeForMarketMaker("mm-1", "exec-test");

        assertThat(report.plannedCount()).isZero();
        assertThat(report.strategyDecisions().getFirst().reason()).isEqualTo("KILL_SWITCH_ENABLED");
        assertThat(fixture.venue.requests).isEmpty();
        assertThat(fixture.published).isEmpty();
    }

    @Test
    @DisplayName("executeForMarketMaker 遇到全域 hedge execution halt 時不送 hedge venue")
    void executeForMarketMakerDoesNotRouteWhenGlobalExecutionHaltIsEnabled() {
        Fixture fixture = new Fixture();
        fixture.riskControls.setMarketMakerHedgeExecutionHalt(true);
        fixture.profileStore.save(profile("mm-1", 9001, true, false));
        fixture.addPosition(9001, "BTCUSDT", "2.000");
        fixture.oracle.update("BTCUSDT", new BigDecimal("100.00"), new BigDecimal("100.00"), "test");

        // 流程：全域停用時不跑 strategy/venue，只回報每個 exposure 被 execution halt 擋下。
        HedgeExecutionReport report = fixture.service.executeForMarketMaker("mm-1", "exec-test");

        assertThat(report.exposureCount()).isEqualTo(1);
        assertThat(report.plannedCount()).isZero();
        assertThat(report.strategyDecisions().getFirst().reason()).isEqualTo("HEDGE_EXECUTION_HALTED");
        assertThat(fixture.venue.requests).isEmpty();
        assertThat(fixture.published).isEmpty();
    }

    private static MarketMakerProfile profile(String marketMakerId, long uid, boolean enabled, boolean killSwitch) {
        return new MarketMakerProfile(
                marketMakerId,
                uid,
                enabled,
                List.of(new MarketMakerRiskLimit(
                        "BTCUSDT",
                        new BigDecimal("150.00"),
                        new BigDecimal("120.00"),
                        new BigDecimal("80.00"),
                        new BigDecimal("0.01"),
                        killSwitch
                ))
        );
    }

    private static final class Fixture {
        private final MemMarketMakerProfileStore profileStore = new MemMarketMakerProfileStore();
        private final MemPositionRepository positionRepository = new MemPositionRepository();
        private final MarkPriceOracleService oracle = new MarkPriceOracleService(new MarkPriceOracleProperties());
        private final FakeHedgeVenueAdapter venue = new FakeHedgeVenueAdapter();
        private final List<HedgeDecisionRecorded> published = new ArrayList<>();
        private final RiskControlsProperties riskControls = new RiskControlsProperties();
        private final MarketMakerHedgeExecutionService service;

        private Fixture() {
            MarketMakerProfileService profileService = new MarketMakerProfileService(profileStore);
            MarketMakerExposureService exposureService = new MarketMakerExposureService(positionRepository, oracle);
            MarketMakerHedgingService hedgingService = new MarketMakerHedgingService(venue, published::add);
            service = new MarketMakerHedgeExecutionService(
                    profileService,
                    exposureService,
                    new MarketMakerHedgeStrategyService(),
                    hedgingService,
                    riskControls
            );
        }

        private void addPosition(long uid, String symbolCode, String qty) {
            Symbol symbol = Symbol.builder().base(symbolCode.replace("USDT", "")).quote("USDT").build();
            positionRepository.save(Position.builder()
                    .uid(uid)
                    .symbol(symbol)
                    .qty(new BigDecimal(qty))
                    .entryPrice(new BigDecimal("100.00"))
                    .build());
        }
    }

    private static final class FakeHedgeVenueAdapter implements HedgeVenueAdapter {
        private final List<HedgeOrderRequest> requests = new ArrayList<>();

        @Override
        public HedgeOrderResult submit(HedgeOrderRequest request) {
            requests.add(request);
            return HedgeOrderResult.accepted("venue-" + requests.size());
        }
    }

    private static final class MemMarketMakerProfileStore implements MarketMakerProfileStore {
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
        public List<MarketMakerProfile> findEnabled() {
            return profiles.values().stream()
                    .filter(MarketMakerProfile::enabled)
                    .toList();
        }
    }

    private static final class MemPositionRepository implements PositionRepository {
        private final Map<String, Position> positions = new LinkedHashMap<>();

        @Override
        public Optional<Position> find(long uid, Symbol symbol) {
            return Optional.ofNullable(positions.get(key(uid, symbol.code())));
        }

        @Override
        public void save(Position position) {
            positions.put(key(position.getUid(), position.getSymbol().code()), position);
        }

        @Override
        public List<Position> findAllByUid(long uid) {
            return positions.values().stream()
                    .filter(position -> position.getUid() == uid)
                    .toList();
        }

        @Override
        public List<Position> findOpenPositions() {
            return positions.values().stream()
                    .filter(position -> position.getQty() != null && position.getQty().signum() != 0)
                    .toList();
        }

        private static String key(long uid, String symbol) {
            return uid + ":" + symbol;
        }
    }
}
