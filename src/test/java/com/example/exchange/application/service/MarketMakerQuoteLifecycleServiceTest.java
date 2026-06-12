/*
 * 檔案用途：測試做市商 quote lifecycle 會將合法 quote 轉成內部 post-only 掛單。
 */
package com.example.exchange.application.service;

import com.example.exchange.domain.event.MarketMakerQuoteDecisionRecorded;
import com.example.exchange.domain.model.dto.MarketMakerProfile;
import com.example.exchange.domain.model.dto.MarketMakerQuoteCommand;
import com.example.exchange.domain.model.dto.MarketMakerQuoteLifecycleReport;
import com.example.exchange.domain.model.dto.MarketMakerQuoteState;
import com.example.exchange.domain.model.dto.MarketMakerRiskLimit;
import com.example.exchange.domain.model.enums.OrderSide;
import com.example.exchange.domain.repository.MarketMakerProfileStore;
import com.example.exchange.domain.repository.MarketMakerQuoteStateStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class MarketMakerQuoteLifecycleServiceTest {

    @Test
    @DisplayName("placeQuote 會把合法雙邊 quote 轉成 BUY/SELL post-only limit orders")
    void placeQuotePlacesBidAndAskOrdersWhenQuoteAccepted() {
        Fixture fixture = new Fixture();
        fixture.profileStore.save(profile(false));

        // 流程：quote validation 通過後，lifecycle service 會送出 bid/ask 兩個 post-only LIMIT leg。
        MarketMakerQuoteLifecycleReport report = fixture.service.placeQuote(quote("99.00", "101.00"));

        assertThat(report.decision().accepted()).isTrue();
        assertThat(report.canceledCount()).isZero();
        assertThat(report.placedCount()).isEqualTo(2);
        assertThat(report.bidOrderId()).isNotNull();
        assertThat(report.askOrderId()).isNotNull();
        assertThat(fixture.quoteStateStore.find("mm-quote-1", "BTCUSDT"))
                .get()
                .satisfies(state -> {
                    assertThat(state.active()).isTrue();
                    assertThat(state.bidOrderId()).isEqualTo(report.bidOrderId());
                    assertThat(state.askOrderId()).isEqualTo(report.askOrderId());
                    assertThat(state.bidVersion()).isEqualTo(1);
                    assertThat(state.askVersion()).isEqualTo(1);
                    assertThat(state.replacedBidOrderId()).isNull();
                    assertThat(state.replacedAskOrderId()).isNull();
                });
        assertThat(fixture.gateway.requests).extracting(PlacedQuoteOrder::side)
                .containsExactly(OrderSide.BUY, OrderSide.SELL);
        assertThat(fixture.published).hasSize(1);
        verify(fixture.pushGatewayService).publishMarket(eq("BTCUSDT"), eq("market-maker.quote"), any(MarketMakerQuoteState.class));
    }

    @Test
    @DisplayName("placeQuote 被 quote validation 拒絕時不送任何內部訂單")
    void placeQuoteDoesNotPlaceOrdersWhenQuoteRejected() {
        Fixture fixture = new Fixture();
        fixture.profileStore.save(profile(true));

        // 流程：kill switch 開啟時只留下 decision audit，不產生任何內部 order side effect。
        MarketMakerQuoteLifecycleReport report = fixture.service.placeQuote(quote("99.00", "101.00"));

        assertThat(report.decision().accepted()).isFalse();
        assertThat(report.decision().reason()).isEqualTo("KILL_SWITCH_ENABLED");
        assertThat(report.canceledCount()).isZero();
        assertThat(report.placedCount()).isZero();
        assertThat(fixture.quoteStateStore.find("mm-quote-1", "BTCUSDT"))
                .get()
                .satisfies(state -> {
                    assertThat(state.active()).isFalse();
                    assertThat(state.reason()).isEqualTo("KILL_SWITCH_ENABLED");
                    assertThat(state.bidOrderId()).isNull();
                    assertThat(state.askOrderId()).isNull();
                });
        assertThat(fixture.gateway.requests).isEmpty();
        verify(fixture.pushGatewayService).publishMarket(eq("BTCUSDT"), eq("market-maker.quote"), any(MarketMakerQuoteState.class));
    }

    @Test
    @DisplayName("placeQuote 在交易 commit 後才推送 market-maker.quote，避免前端刷新讀到舊 quote")
    void placeQuotePublishesAfterCommitWhenTransactionSynchronizationIsActive() {
        Fixture fixture = new Fixture();
        fixture.profileStore.save(profile(false));

        TransactionSynchronizationManager.initSynchronization();
        try {
            // 流程：controller 交易尚未 commit 前不推 WebSocket；commit 後前端再查 REST 才能讀到新 quote state。
            fixture.service.placeQuote(quote("99.00", "101.00"));

            verify(fixture.pushGatewayService, never()).publishMarket(eq("BTCUSDT"), eq("market-maker.quote"), any(MarketMakerQuoteState.class));
            TransactionSynchronizationManager.getSynchronizations()
                    .forEach(synchronization -> synchronization.afterCommit());
            verify(fixture.pushGatewayService).publishMarket(eq("BTCUSDT"), eq("market-maker.quote"), any(MarketMakerQuoteState.class));
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    @DisplayName("placeQuote 會先撤同一做市商同 symbol 的舊 quote orders 再掛 replacement")
    void placeQuoteCancelsStaleQuoteOrdersBeforeReplacement() {
        Fixture fixture = new Fixture();
        fixture.profileStore.save(profile(false));
        fixture.gateway.cancelCount = 2;

        // 流程：新的 quote accepted 時，先清掉舊 bid/ask quote，避免 stale quote 留在 book 上。
        MarketMakerQuoteLifecycleReport report = fixture.service.placeQuote(quote("100.00", "102.00"));

        assertThat(report.decision().accepted()).isTrue();
        assertThat(report.canceledCount()).isEqualTo(2);
        assertThat(report.placedCount()).isEqualTo(2);
        assertThat(fixture.gateway.cancelRequests).containsExactly("quote-ref-1");
        assertThat(fixture.gateway.requests).extracting(PlacedQuoteOrder::side)
                .containsExactly(OrderSide.BUY, OrderSide.SELL);
    }

    @Test
    @DisplayName("placeQuoteLadder 只撤舊 quote 一次，然後掛出多層 BUY/SELL quote")
    void placeQuoteLadderCancelsOnceAndPlacesMultipleLevels() {
        Fixture fixture = new Fixture();
        fixture.profileStore.save(profile(false));
        fixture.gateway.cancelCount = 2;

        // 流程：多層做市 ladder 不能逐層呼叫單 quote，否則下一層會撤掉上一層；這裡只允許先撤一次再批量掛單。
        List<MarketMakerQuoteLifecycleReport> reports = fixture.service.placeQuoteLadder(List.of(
                quote("99.00", "101.00"),
                new MarketMakerQuoteCommand(
                        "mm-quote-1",
                        9101,
                        "BTCUSDT",
                        new BigDecimal("98.90"),
                        new BigDecimal("1.000"),
                        new BigDecimal("101.10"),
                        new BigDecimal("1.000"),
                        "quote-ref-2"
                )
        ));

        assertThat(reports).hasSize(2);
        assertThat(reports).extracting(MarketMakerQuoteLifecycleReport::placedCount)
                .containsExactly(2, 2);
        assertThat(fixture.gateway.cancelRequests).containsExactly("quote-ref-1");
        assertThat(fixture.gateway.requests).extracting(PlacedQuoteOrder::side)
                .containsExactly(OrderSide.BUY, OrderSide.SELL, OrderSide.BUY, OrderSide.SELL);
        assertThat(fixture.quoteStateStore.find("mm-quote-1", "BTCUSDT"))
                .get()
                .satisfies(state -> {
                    // 對外 quote state 保存最接近盤口的一層，讓前台 top-of-book 能標記做市商正在掛單。
                    assertThat(state.refId()).isEqualTo("quote-ref-1");
                    assertThat(state.bidPrice()).isEqualByComparingTo("99.00");
                    assertThat(state.askPrice()).isEqualByComparingTo("101.00");
                });
    }

    @Test
    @DisplayName("placeQuote 連續 replacement 會遞增 bid/ask version 並保留上一輪 order id")
    void placeQuoteIncrementsPerSideVersionsAndTracksReplacedOrderIds() {
        Fixture fixture = new Fixture();
        fixture.profileStore.save(profile(false));
        MarketMakerQuoteLifecycleReport first = fixture.service.placeQuote(quote("99.00", "101.00"));
        fixture.gateway.cancelCount = 2;

        // 流程：新的 quote 取代舊 quote 時，state 必須保留上一輪 bid/ask id 供重啟後 reconciliation 追查。
        MarketMakerQuoteLifecycleReport second = fixture.service.placeQuote(quote("100.00", "102.00"));

        assertThat(fixture.quoteStateStore.find("mm-quote-1", "BTCUSDT"))
                .get()
                .satisfies(state -> {
                    assertThat(state.bidOrderId()).isEqualTo(second.bidOrderId());
                    assertThat(state.askOrderId()).isEqualTo(second.askOrderId());
                    assertThat(state.bidVersion()).isEqualTo(2);
                    assertThat(state.askVersion()).isEqualTo(2);
                    assertThat(state.replacedBidOrderId()).isEqualTo(first.bidOrderId());
                    assertThat(state.replacedAskOrderId()).isEqualTo(first.askOrderId());
                });
    }

    @Test
    @DisplayName("placeQuote 會拒絕 uid 與 profile 不一致的 quote command")
    void placeQuoteRejectsUidMismatch() {
        Fixture fixture = new Fixture();
        fixture.profileStore.save(profile(false));

        // 流程：quote command 的 uid 必須與 durable market-maker profile 一致，避免借其他帳戶掛 quote。
        assertThatThrownBy(() -> fixture.service.placeQuote(new MarketMakerQuoteCommand(
                "mm-quote-1",
                9999,
                "BTCUSDT",
                new BigDecimal("99.00"),
                new BigDecimal("1.000"),
                new BigDecimal("101.00"),
                new BigDecimal("1.000"),
                "quote-ref-1"
        ))).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("uid mismatch");
        assertThat(fixture.gateway.requests).isEmpty();
    }

    private static MarketMakerProfile profile(boolean killSwitch) {
        return new MarketMakerProfile(
                "mm-quote-1",
                9101,
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

    private static MarketMakerQuoteCommand quote(String bidPrice, String askPrice) {
        return new MarketMakerQuoteCommand(
                "mm-quote-1",
                9101,
                "BTCUSDT",
                new BigDecimal(bidPrice),
                new BigDecimal("1.000"),
                new BigDecimal(askPrice),
                new BigDecimal("1.000"),
                "quote-ref-1"
        );
    }

    private static final class Fixture {
        private final MemProfileStore profileStore = new MemProfileStore();
        private final MemQuoteStateStore quoteStateStore = new MemQuoteStateStore();
        private final RecordingQuoteOrderGateway gateway = new RecordingQuoteOrderGateway();
        private final List<MarketMakerQuoteDecisionRecorded> published = new ArrayList<>();
        private final PushGatewayService pushGatewayService = mock(PushGatewayService.class);
        private final MarketMakerQuoteLifecycleService service = new MarketMakerQuoteLifecycleService(
                new MarketMakerProfileService(profileStore),
                new MarketMakerQuoteService(published::add),
                gateway,
                quoteStateStore,
                pushGatewayService
        );
    }

    private record PlacedQuoteOrder(MarketMakerQuoteCommand command, OrderSide side, UUID orderId) {
    }

    private static final class RecordingQuoteOrderGateway implements MarketMakerQuoteOrderGateway {
        private final List<PlacedQuoteOrder> requests = new ArrayList<>();
        private final List<String> cancelRequests = new ArrayList<>();
        private int cancelCount;

        @Override
        public int cancelOpenQuoteOrders(MarketMakerQuoteCommand command) {
            cancelRequests.add(command.refId());
            return cancelCount;
        }

        @Override
        public boolean cancelOrder(UUID orderId) {
            return true;
        }

        @Override
        public UUID placePostOnlyLimit(MarketMakerQuoteCommand command, OrderSide side) {
            UUID orderId = UUID.randomUUID();
            requests.add(new PlacedQuoteOrder(command, side, orderId));
            return orderId;
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
        public List<MarketMakerProfile> findEnabled() {
            return profiles.values().stream()
                    .filter(MarketMakerProfile::enabled)
                    .toList();
        }
    }

    private static final class MemQuoteStateStore implements MarketMakerQuoteStateStore {
        private final Map<String, MarketMakerQuoteState> states = new LinkedHashMap<>();

        @Override
        public void save(MarketMakerQuoteState state) {
            states.put(key(state.marketMakerId(), state.symbol()), state);
        }

        @Override
        public Optional<MarketMakerQuoteState> find(String marketMakerId, String symbol) {
            return Optional.ofNullable(states.get(key(marketMakerId, symbol)));
        }

        @Override
        public List<MarketMakerQuoteState> findByMarketMakerId(String marketMakerId, int limit) {
            return states.values().stream()
                    .filter(state -> state.marketMakerId().equals(marketMakerId))
                    .limit(limit)
                    .toList();
        }

        @Override
        public List<MarketMakerQuoteState> findActive(int limit) {
            return states.values().stream()
                    .filter(MarketMakerQuoteState::active)
                    .limit(limit)
                    .toList();
        }

        private static String key(String marketMakerId, String symbol) {
            return marketMakerId.trim() + ":" + symbol.trim().toUpperCase();
        }
    }
}
