/*
 * 檔案用途：測試 REST-mode startup matching book recovery，避免 open orders 與 depth 分裂。
 */
package com.example.exchange.application.service;

import com.example.exchange.domain.model.dto.MatchingEngineSnapshot;
import com.example.exchange.domain.model.entity.Order;
import com.example.exchange.domain.model.entity.Symbol;
import com.example.exchange.domain.model.entity.SymbolConfig;
import com.example.exchange.domain.model.enums.OrderSide;
import com.example.exchange.domain.model.enums.OrderType;
import com.example.exchange.domain.repository.MatchingCommandLog;
import com.example.exchange.domain.repository.MatchingReplayValidationReportStore;
import com.example.exchange.domain.repository.MatchingSnapshotStore;
import com.example.exchange.domain.repository.OrderRepository;
import com.example.exchange.domain.repository.SymbolConfigRepository;
import com.example.exchange.infra.config.MatchingBookRecoveryProperties;
import com.example.exchange.infra.matching.InMemoryMatchingCommandLog;
import com.example.exchange.infra.matching.InMemoryMatchingEngine;
import com.example.exchange.infra.matching.InMemoryMatchingEventLog;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MatchingBookRecoveryServiceTest {

    @Test
    @DisplayName("startup recovery 會用 persisted open orders fallback 重建空的 runtime order book")
    /**
     * 流程：durable command log/snapshot 為空，但 Redis-like repository 有 open order -> recovery 後 depth 有 ask。
     */
    void recoversRuntimeBookFromPersistedOpenOrdersWhenDurableReplayIsEmpty() {
        Symbol symbol = Symbol.builder().base("BTC").quote("USDT").priceScale(2).qtyScale(3).build();
        Order openAsk = Order.builder()
                .id(UUID.randomUUID())
                .uid(7)
                .symbol(symbol)
                .side(OrderSide.SELL)
                .type(OrderType.LIMIT)
                .price(new BigDecimal("101.00"))
                .qty(new BigDecimal("2.000"))
                .origQty(new BigDecimal("2.000"))
                .status(Order.Status.NEW)
                .build();

        InMemoryMatchingEngine engine = new InMemoryMatchingEngine(new InMemoryMatchingCommandLog(), new InMemoryMatchingEventLog());
        MatchingBookRecoveryService service = service(engine, List.of(openAsk), List.of(config("BTCUSDT")));

        MatchingBookRecoveryService.MatchingBookRecoveryResult result = service.recoverSymbol("BTCUSDT");

        assertThat(result.source()).isEqualTo("OPEN_ORDER_FALLBACK");
        assertThat(result.fallbackRestoredOrders()).isEqualTo(1);
        assertThat(engine.snapshot("BTCUSDT", 10).asks())
                .extracting(level -> level.price())
                .containsExactly(new BigDecimal("101.00"));
        engine.shutdown();
    }

    private static MatchingBookRecoveryService service(
            InMemoryMatchingEngine engine,
            List<Order> openOrders,
            List<SymbolConfig> configs
    ) {
        MatchingBookRecoveryProperties properties = new MatchingBookRecoveryProperties();
        MatchingCommandLog commandLog = new InMemoryMatchingCommandLog();
        MatchingRecoveryService durableRecovery = new MatchingRecoveryService(
                engine,
                commandLog,
                new EmptySnapshotStore(),
                new EmptyReportStore()
        );
        return new MatchingBookRecoveryService(
                properties,
                durableRecovery,
                engine,
                new MemOrderRepository(openOrders),
                new MemSymbolConfigRepository(configs)
        );
    }

    private static SymbolConfig config(String symbol) {
        return SymbolConfig.builder()
                .symbol(symbol)
                .baseAsset(symbol.replace("USDT", ""))
                .quoteAsset("USDT")
                .priceTick(new BigDecimal("0.01"))
                .lotSize(new BigDecimal("0.001"))
                .tradingEnabled(true)
                .build();
    }

    private static final class MemOrderRepository implements OrderRepository {
        private final List<Order> orders;

        private MemOrderRepository(List<Order> orders) {
            this.orders = new ArrayList<>(orders);
        }

        @Override
        public Optional<Order> findById(UUID id) {
            return orders.stream().filter(order -> order.getId().equals(id)).findFirst();
        }

        @Override
        public void save(Order order) {
            orders.add(order);
        }

        @Override
        public List<Order> openOrders(long uid) {
            return orders.stream().filter(order -> order.getUid() == uid).toList();
        }

        @Override
        public List<Order> openOrders() {
            return List.copyOf(orders);
        }

        @Override
        public List<Order> findOpenOrders(Long uid, String symbol) {
            return openOrders(uid).stream()
                    .filter(order -> order.getSymbol().code().equals(symbol))
                    .toList();
        }

        @Override
        public List<Order> findAllOrders(Long uid, String symbol) {
            return findOpenOrders(uid, symbol);
        }
    }

    private record MemSymbolConfigRepository(List<SymbolConfig> configs) implements SymbolConfigRepository {
        @Override
        public Optional<SymbolConfig> findBySymbol(String symbol) {
            return configs.stream().filter(config -> config.getSymbol().equals(symbol)).findFirst();
        }

        @Override
        public List<SymbolConfig> findAll() {
            return configs;
        }
    }

    private static final class EmptySnapshotStore implements MatchingSnapshotStore {
        @Override
        public void save(MatchingEngineSnapshot snapshot) {
        }

        @Override
        public Optional<MatchingEngineSnapshot> latest(String symbolCode) {
            return Optional.empty();
        }
    }

    private static final class EmptyReportStore implements MatchingReplayValidationReportStore {
        @Override
        public void save(com.example.exchange.domain.model.dto.MatchingReplayValidationReport report) {
        }

        @Override
        public List<com.example.exchange.domain.model.dto.MatchingReplayValidationReport> findBySymbol(String symbolCode, int limit) {
            return List.of();
        }
    }
}
