/*
 * 檔案用途：測試做市商 active quote state 與 open order 對帳。
 */
package com.example.exchange.application.service;

import com.example.exchange.domain.model.dto.MarketMakerQuoteReconciliationReport;
import com.example.exchange.domain.model.dto.MarketMakerQuoteState;
import com.example.exchange.domain.model.entity.Order;
import com.example.exchange.domain.model.entity.Symbol;
import com.example.exchange.domain.model.enums.OrderSide;
import com.example.exchange.domain.model.enums.OrderType;
import com.example.exchange.domain.repository.MarketMakerQuoteStateStore;
import com.example.exchange.domain.repository.OrderRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MarketMakerQuoteReconciliationServiceTest {

    @Test
    @DisplayName("reconcileActiveQuotes 在 tracked bid/ask 都是 open order 時不報 issue")
    void reconcileActiveQuotesPassesWhenTrackedOrdersAreOpen() {
        Fixture fixture = new Fixture();
        Order bid = order(OrderSide.BUY, "mmq:mm-1:ref-1:buy");
        Order ask = order(OrderSide.SELL, "mmq:mm-1:ref-1:sell");
        fixture.orderRepository.orders.addAll(List.of(bid, ask));
        fixture.quoteStateStore.states.add(state(bid.getId(), ask.getId()));

        // 場景：durable quote state 記錄的 bid/ask order id 都仍在 open orders 中，重啟後 ownership 可對齊。
        MarketMakerQuoteReconciliationReport report = fixture.service.reconcileActiveQuotes(50);

        assertThat(report.checkedStates()).isEqualTo(1);
        assertThat(report.issueCount()).isZero();
        assertThat(report.issues()).isEmpty();
    }

    @Test
    @DisplayName("reconcileActiveQuotes 會報告 state 追蹤的 quote order 已不在 open orders")
    void reconcileActiveQuotesReportsTrackedOrderNotOpen() {
        Fixture fixture = new Fixture();
        Order bid = order(OrderSide.BUY, "mmq:mm-1:ref-1:buy");
        UUID missingAskId = UUID.randomUUID();
        fixture.orderRepository.orders.add(bid);
        fixture.quoteStateStore.states.add(state(bid.getId(), missingAskId));

        // 場景：state 仍指向舊 ask order，但 order repository 已沒有 open ask，operator 需要知道 state/book 不一致。
        MarketMakerQuoteReconciliationReport report = fixture.service.reconcileActiveQuotes(50);

        assertThat(report.issueCount()).isEqualTo(1);
        assertThat(report.issues().getFirst().side()).isEqualTo("SELL");
        assertThat(report.issues().getFirst().orderId()).isEqualTo(missingAskId);
        assertThat(report.issues().getFirst().reason()).isEqualTo("TRACKED_ORDER_NOT_OPEN");
    }

    @Test
    @DisplayName("reconcileActiveQuotes 會報告同做市商前綴但 state 未追蹤的 open quote order")
    void reconcileActiveQuotesReportsUntrackedOpenQuoteOrder() {
        Fixture fixture = new Fixture();
        Order bid = order(OrderSide.BUY, "mmq:mm-1:ref-1:buy");
        Order ask = order(OrderSide.SELL, "mmq:mm-1:ref-1:sell");
        Order stale = order(OrderSide.BUY, "mmq:mm-1:old-ref:buy");
        Order manual = order(OrderSide.BUY, "manual-order");
        fixture.orderRepository.orders.addAll(List.of(bid, ask, stale, manual));
        fixture.quoteStateStore.states.add(state(bid.getId(), ask.getId()));

        // 場景：額外殘留的 mmq 前綴 open order 代表 stale cleanup/restore 有落差，普通手動單不應被誤報。
        MarketMakerQuoteReconciliationReport report = fixture.service.reconcileActiveQuotes(50);

        assertThat(report.issueCount()).isEqualTo(1);
        assertThat(report.issues().getFirst().orderId()).isEqualTo(stale.getId());
        assertThat(report.issues().getFirst().clientOrderId()).isEqualTo("mmq:mm-1:old-ref:buy");
        assertThat(report.issues().getFirst().reason()).isEqualTo("UNTRACKED_OPEN_QUOTE_ORDER");
    }

    @Test
    @DisplayName("reconcileActiveQuotes 拒絕無界限查詢 limit")
    void reconcileActiveQuotesRejectsInvalidLimit() {
        Fixture fixture = new Fixture();

        // 場景：operator reconciliation 必須有查詢上限，避免一次掃太多 active quote state。
        assertThatThrownBy(() -> fixture.service.reconcileActiveQuotes(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("between 1 and 500");
        assertThatThrownBy(() -> fixture.service.reconcileActiveQuotes(501))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("between 1 and 500");
    }

    private static MarketMakerQuoteState state(UUID bidOrderId, UUID askOrderId) {
        return new MarketMakerQuoteState(
                "mm-1",
                9101,
                "BTCUSDT",
                "ref-1",
                true,
                true,
                "ACCEPTED",
                0,
                bidOrderId,
                askOrderId,
                1,
                1,
                null,
                null,
                Instant.parse("2026-06-01T00:00:00Z")
        );
    }

    private static Order order(OrderSide side, String clientOrderId) {
        return Order.builder()
                .id(UUID.randomUUID())
                .uid(9101)
                .symbol(Symbol.builder().base("BTC").quote("USDT").build())
                .side(side)
                .type(OrderType.LIMIT)
                .price(new BigDecimal("100.00"))
                .qty(new BigDecimal("1.000"))
                .origQty(new BigDecimal("1.000"))
                .clientOrderId(clientOrderId)
                .status(Order.Status.NEW)
                .build();
    }

    private static final class Fixture {
        private final MemQuoteStateStore quoteStateStore = new MemQuoteStateStore();
        private final MemOrderRepository orderRepository = new MemOrderRepository();
        private final MarketMakerQuoteReconciliationService service =
                new MarketMakerQuoteReconciliationService(quoteStateStore, orderRepository);
    }

    private static final class MemQuoteStateStore implements MarketMakerQuoteStateStore {
        private final List<MarketMakerQuoteState> states = new ArrayList<>();

        @Override
        public void save(MarketMakerQuoteState state) {
            states.add(state);
        }

        @Override
        public Optional<MarketMakerQuoteState> find(String marketMakerId, String symbol) {
            return states.stream()
                    .filter(state -> state.marketMakerId().equals(marketMakerId))
                    .filter(state -> state.symbol().equals(symbol))
                    .findFirst();
        }

        @Override
        public List<MarketMakerQuoteState> findByMarketMakerId(String marketMakerId, int limit) {
            return states.stream()
                    .filter(state -> state.marketMakerId().equals(marketMakerId))
                    .limit(limit)
                    .toList();
        }

        @Override
        public List<MarketMakerQuoteState> findActive(int limit) {
            return states.stream()
                    .filter(MarketMakerQuoteState::active)
                    .limit(limit)
                    .toList();
        }
    }

    private static final class MemOrderRepository implements OrderRepository {
        private final List<Order> orders = new ArrayList<>();

        @Override
        public Optional<Order> findById(UUID id) {
            return orders.stream()
                    .filter(order -> order.getId().equals(id))
                    .findFirst();
        }

        @Override
        public void save(Order order) {
        }

        @Override
        public List<Order> openOrders(long uid) {
            return orders.stream()
                    .filter(order -> order.getUid() == uid)
                    .filter(order -> order.getStatus() == Order.Status.NEW || order.getStatus() == Order.Status.PARTIALLY_FILLED)
                    .toList();
        }

        @Override
        public List<Order> findOpenOrders(Long uid, String symbol) {
            return openOrders(uid).stream()
                    .filter(order -> order.getSymbol().code().equals(symbol))
                    .toList();
        }

        @Override
        public List<Order> findAllOrders(Long uid, String symbol) {
            return orders.stream()
                    .filter(order -> order.getUid() == uid)
                    .filter(order -> order.getSymbol().code().equals(symbol))
                    .toList();
        }
    }
}
