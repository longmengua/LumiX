/*
 * 檔案用途：測試做市商 exposure aggregation 與 hedging risk controls。
 */
package com.example.exchange.application.service;

import com.example.exchange.domain.event.HedgeDecisionRecorded;
import com.example.exchange.domain.model.dto.HedgeDecision;
import com.example.exchange.domain.model.dto.HedgeDecisionAuditRecord;
import com.example.exchange.domain.model.dto.HedgeOrderRequest;
import com.example.exchange.domain.model.dto.HedgeOrderResult;
import com.example.exchange.domain.model.dto.MarketMakerExposure;
import com.example.exchange.domain.model.dto.MarketMakerProfile;
import com.example.exchange.domain.model.dto.MarketMakerRiskLimit;
import com.example.exchange.domain.model.entity.Position;
import com.example.exchange.domain.model.entity.Symbol;
import com.example.exchange.domain.model.enums.OrderSide;
import com.example.exchange.domain.repository.HedgeDecisionAuditStore;
import com.example.exchange.domain.repository.PositionRepository;
import com.example.exchange.domain.service.HedgeVenueAdapter;
import com.example.exchange.infra.config.MarkPriceOracleProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class MarketMakerHedgingServiceTest {

    @Test
    @DisplayName("exposures 會用 mark price 彙總做市商持倉名目")
    void exposuresAggregateMarketMakerInventory() {
        MemPositionRepository positionRepository = new MemPositionRepository();
        MarkPriceOracleService oracle = new MarkPriceOracleService(new MarkPriceOracleProperties());
        oracle.update("BTCUSDT", new BigDecimal("100.00"), new BigDecimal("100.00"), "test");
        Symbol symbol = Symbol.builder().base("BTC").quote("USDT").priceScale(2).qtyScale(3).build();
        positionRepository.save(Position.builder()
                .uid(9001)
                .symbol(symbol)
                .qty(new BigDecimal("-2.000"))
                .entryPrice(new BigDecimal("99.00"))
                .build());
        MarketMakerProfile profile = profile(false);

        // 流程：做市商 uid 有 -2 BTC position，mark=100，exposure notional 應為 -200。
        List<MarketMakerExposure> exposures = new MarketMakerExposureService(positionRepository, oracle)
                .exposures(profile);

        assertThat(exposures).hasSize(1);
        assertThat(exposures.getFirst().marketMakerId()).isEqualTo("mm-1");
        assertThat(exposures.getFirst().notional()).isEqualByComparingTo("-200.00000");
    }

    @Test
    @DisplayName("kill switch 會拒絕 hedge 且不送 venue，但仍發布 audit event")
    void killSwitchRejectsHedgeBeforeVenueSubmit() {
        FakeHedgeVenueAdapter adapter = new FakeHedgeVenueAdapter();
        List<HedgeDecisionRecorded> published = new ArrayList<>();
        MarketMakerHedgingService service = new MarketMakerHedgingService(adapter, published::add);
        MemHedgeDecisionAuditStore auditStore = new MemHedgeDecisionAuditStore();
        service.setAuditStore(auditStore);

        // 流程：profile risk limit 開 kill switch，hedge decision 應拒絕且 adapter 不被呼叫。
        HedgeDecision decision = service.hedge(profile(true), request("100.00", "100.50"));

        assertThat(decision.accepted()).isFalse();
        assertThat(decision.reason()).isEqualTo("KILL_SWITCH_ENABLED");
        assertThat(adapter.requests).isEmpty();
        assertThat(published).extracting(HedgeDecisionRecorded::reason)
                .containsExactly("KILL_SWITCH_ENABLED");
        assertThat(auditStore.records).extracting(HedgeDecisionAuditRecord::reason)
                .containsExactly("KILL_SWITCH_ENABLED");
    }

    @Test
    @DisplayName("slippage 超過限制會拒絕 hedge")
    void maxSlippageRejectsHedge() {
        FakeHedgeVenueAdapter adapter = new FakeHedgeVenueAdapter();
        List<HedgeDecisionRecorded> published = new ArrayList<>();
        MarketMakerHedgingService service = new MarketMakerHedgingService(adapter, published::add);

        // 流程：limit price 比 reference price 高 3%，超過 1% max slippage，必須拒絕。
        HedgeDecision decision = service.hedge(profile(false), request("100.00", "103.00"));

        assertThat(decision.accepted()).isFalse();
        assertThat(decision.reason()).isEqualTo("MAX_SLIPPAGE_EXCEEDED");
        assertThat(adapter.requests).isEmpty();
        assertThat(published).hasSize(1);
    }

    @Test
    @DisplayName("通過風控的 hedge 會送往 venue 並記錄 venue order id")
    void acceptedHedgeRoutesToVenueAndPublishesAuditEvent() {
        FakeHedgeVenueAdapter adapter = new FakeHedgeVenueAdapter();
        List<HedgeDecisionRecorded> published = new ArrayList<>();
        MarketMakerHedgingService service = new MarketMakerHedgingService(adapter, published::add);
        MemHedgeDecisionAuditStore auditStore = new MemHedgeDecisionAuditStore();
        service.setAuditStore(auditStore);

        // 流程：slippage 在限制內，service 應呼叫 adapter 並把 venue order id 放進 audit event。
        HedgeDecision decision = service.hedge(profile(false), request("100.00", "100.50"));

        assertThat(decision.accepted()).isTrue();
        assertThat(adapter.requests).hasSize(1);
        assertThat(published.getFirst().venueOrderId()).isEqualTo("venue-1");
        assertThat(auditStore.findByRefId("trade-ref-1").getFirst().venueOrderId()).isEqualTo("venue-1");
    }

    private static MarketMakerProfile profile(boolean killSwitch) {
        return new MarketMakerProfile(
                "mm-1",
                9001,
                true,
                List.of(new MarketMakerRiskLimit(
                        "BTCUSDT",
                        new BigDecimal("1000000"),
                        new BigDecimal("1000000"),
                        new BigDecimal("10000"),
                        new BigDecimal("0.01"),
                        killSwitch
                ))
        );
    }

    private static HedgeOrderRequest request(String referencePrice, String limitPrice) {
        return new HedgeOrderRequest(
                "mm-1",
                9001,
                "BTCUSDT",
                OrderSide.BUY,
                new BigDecimal("1.000"),
                new BigDecimal(referencePrice),
                new BigDecimal(limitPrice),
                "trade-ref-1"
        );
    }

    private static class FakeHedgeVenueAdapter implements HedgeVenueAdapter {
        private final List<HedgeOrderRequest> requests = new ArrayList<>();

        @Override
        public HedgeOrderResult submit(HedgeOrderRequest request) {
            requests.add(request);
            return HedgeOrderResult.accepted("venue-" + requests.size());
        }
    }

    private static class MemHedgeDecisionAuditStore implements HedgeDecisionAuditStore {
        private final List<HedgeDecisionAuditRecord> records = new ArrayList<>();

        @Override
        public void append(HedgeDecisionAuditRecord record) {
            records.add(record);
        }

        @Override
        public List<HedgeDecisionAuditRecord> findByMarketMakerId(String marketMakerId, int limit) {
            return records.stream()
                    .filter(record -> marketMakerId.equals(record.marketMakerId()))
                    .limit(limit)
                    .toList();
        }

        @Override
        public List<HedgeDecisionAuditRecord> findByRefId(String refId) {
            return records.stream()
                    .filter(record -> refId.equals(record.refId()))
                    .toList();
        }
    }

    private static class MemPositionRepository implements PositionRepository {
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
