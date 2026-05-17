package com.example.exchange.application.service;

import com.example.exchange.application.command.PlaceOrderCommand;
import com.example.exchange.application.usecase.PlaceOrderUseCase;
import com.example.exchange.domain.event.TradeExecuted;
import com.example.exchange.domain.model.entity.Account;
import com.example.exchange.domain.model.entity.Order;
import com.example.exchange.domain.model.entity.Position;
import com.example.exchange.domain.model.entity.Symbol;
import com.example.exchange.domain.model.entity.WalletLedgerEntry;
import com.example.exchange.domain.model.enums.OrderSide;
import com.example.exchange.domain.model.enums.OrderType;
import com.example.exchange.domain.repository.AccountRepository;
import com.example.exchange.domain.repository.EventStore;
import com.example.exchange.domain.repository.IdempotencyRepository;
import com.example.exchange.domain.repository.OrderRepository;
import com.example.exchange.domain.repository.PositionRepository;
import com.example.exchange.domain.repository.WalletLedgerRepository;
import com.example.exchange.infra.config.DefaultSymbolConfigRepository;
import com.example.exchange.infra.matching.InMemoryMatchingEngine;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

class OrderAccountingIntegrationTest {

    @Test
    void fillUpdatesPositionsFeesLedgerAndMarketData() {
        MemAccountRepository accountRepo = new MemAccountRepository();
        MemWalletLedgerRepository ledgerRepo = new MemWalletLedgerRepository();
        MemPositionRepository positionRepo = new MemPositionRepository();
        MemOrderRepository orderRepo = new MemOrderRepository();
        MemEventStore eventStore = new MemEventStore();
        List<TradeExecuted> published = new ArrayList<>();

        DefaultSymbolConfigRepository symbolRepo = new DefaultSymbolConfigRepository();
        InMemoryMatchingEngine matchingEngine = new InMemoryMatchingEngine();
        WalletLedgerService walletLedgerService = new WalletLedgerService(accountRepo, ledgerRepo);
        RiskService riskService = new RiskService(accountRepo, positionRepo, matchingEngine, walletLedgerService);
        MarketDataService marketDataService = new MarketDataService();
        IdempotencyService idempotencyService = new IdempotencyService(new MemIdempotencyRepository());
        OrderService orderService = new OrderService(
                matchingEngine,
                positionRepo,
                eventStore,
                published::add,
                orderRepo,
                symbolRepo,
                walletLedgerService,
                new FeeService(),
                riskService,
                marketDataService,
                idempotencyService
        );
        PlaceOrderUseCase placeOrderUseCase = new PlaceOrderUseCase(orderService, riskService, symbolRepo);

        walletLedgerService.deposit(1, "USDT", new BigDecimal("10000"), "deposit-1");
        walletLedgerService.deposit(2, "USDT", new BigDecimal("10000"), "deposit-2");

        placeOrderUseCase.handle(command(2, OrderSide.SELL, "100.00", "1.000"));
        placeOrderUseCase.handle(command(1, OrderSide.BUY, "100.00", "1.000"));

        Symbol symbol = Symbol.builder().base("BTC").quote("USDT").priceScale(1).qtyScale(3).build();
        Position buyerPosition = positionRepo.find(1, symbol).orElseThrow();
        Position sellerPosition = positionRepo.find(2, symbol).orElseThrow();
        Account buyerAccount = accountRepo.findByUid(1).orElseThrow();
        Account sellerAccount = accountRepo.findByUid(2).orElseThrow();

        assertThat(buyerPosition.getQty()).isEqualByComparingTo("1.000");
        assertThat(sellerPosition.getQty()).isEqualByComparingTo("-1.000");
        assertThat(buyerPosition.getMargin()).isEqualByComparingTo("5.000000000000000000");
        assertThat(sellerPosition.getMargin()).isEqualByComparingTo("5.000000000000000000");
        assertThat(buyerPosition.getFeePaid()).isEqualByComparingTo("0.050000000000000000");
        assertThat(buyerPosition.getRebateEarned()).isEqualByComparingTo("0.005000000000000000");
        assertThat(sellerPosition.getFeePaid()).isEqualByComparingTo("0.020000000000000000");

        assertThat(buyerAccount.crossBalance()).isEqualByComparingTo("9999.955000000000000000");
        assertThat(buyerAccount.crossOrderHold()).isEqualByComparingTo("0");
        assertThat(buyerAccount.crossPositionMargin()).isEqualByComparingTo("5.000000000000000000");
        assertThat(sellerAccount.crossBalance()).isEqualByComparingTo("9999.980000000000000000");
        assertThat(sellerAccount.crossOrderHold()).isEqualByComparingTo("0");
        assertThat(sellerAccount.crossPositionMargin()).isEqualByComparingTo("5.000000000000000000");

        assertThat(orderRepo.findAllOrders(1L, "BTCUSDT")).allMatch(o -> o.getStatus() == Order.Status.FILLED);
        assertThat(orderRepo.findAllOrders(2L, "BTCUSDT")).allMatch(o -> o.getStatus() == Order.Status.FILLED);
        assertThat(ledgerRepo.findByUid(1)).extracting(WalletLedgerEntry::getReason)
                .contains("order_reserve", "position_margin_increase", "trade_fee");
        assertThat(published).hasSize(2);
        assertThat(marketDataService.ticker("BTCUSDT")).isPresent();
        assertThat(marketDataService.trades("BTCUSDT", 10)).hasSize(1);

        matchingEngine.shutdown();
    }

    private static PlaceOrderCommand command(long uid, OrderSide side, String price, String qty) {
        return new PlaceOrderCommand(
                uid,
                "BTCUSDT",
                side,
                OrderType.LIMIT,
                new BigDecimal(price),
                new BigDecimal(qty),
                20,
                "CROSS",
                null,
                "GTC",
                false,
                false
        );
    }

    private static class MemAccountRepository implements AccountRepository {
        private final Map<Long, Account> accounts = new LinkedHashMap<>();

        @Override
        public Optional<Account> findByUid(long uid) {
            return Optional.ofNullable(accounts.get(uid));
        }

        @Override
        public void save(Account account) {
            accounts.put(account.uid(), account);
        }
    }

    private static class MemWalletLedgerRepository implements WalletLedgerRepository {
        private final List<WalletLedgerEntry> entries = new ArrayList<>();

        @Override
        public void append(WalletLedgerEntry entry) {
            assertThat(entry.isBalanced()).isTrue();
            entries.add(entry);
        }

        @Override
        public List<WalletLedgerEntry> findByUid(long uid) {
            return entries.stream().filter(entry -> entry.getUid() == uid).toList();
        }

        @Override
        public List<WalletLedgerEntry> findByRefId(String refId) {
            return entries.stream().filter(entry -> refId.equals(entry.getRefId())).toList();
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
            return positions.values().stream().filter(position -> position.getUid() == uid).toList();
        }

        private static String key(long uid, String symbol) {
            return uid + ":" + symbol;
        }
    }

    private static class MemOrderRepository implements OrderRepository {
        private final Map<UUID, Order> orders = new LinkedHashMap<>();

        @Override
        public Optional<Order> findById(UUID id) {
            return Optional.ofNullable(orders.get(id));
        }

        @Override
        public void save(Order order) {
            orders.put(order.getId(), order);
        }

        @Override
        public List<Order> openOrders(long uid) {
            return orders.values().stream()
                    .filter(order -> order.getUid() == uid)
                    .filter(order -> order.getStatus() == Order.Status.NEW
                            || order.getStatus() == Order.Status.PARTIALLY_FILLED)
                    .toList();
        }

        @Override
        public List<Order> findOpenOrders(Long uid, String symbol) {
            return openOrders(uid).stream()
                    .filter(order -> order.getSymbol().code().equalsIgnoreCase(symbol))
                    .toList();
        }

        @Override
        public List<Order> findAllOrders(Long uid, String symbol) {
            return orders.values().stream()
                    .filter(order -> order.getUid() == uid)
                    .filter(order -> order.getSymbol().code().equalsIgnoreCase(symbol))
                    .toList();
        }
    }

    private static class MemEventStore implements EventStore {
        private final AtomicLong seq = new AtomicLong();

        @Override
        public long append(TradeExecuted event) {
            return seq.incrementAndGet();
        }

        @Override
        public long lastSeq(long uid) {
            return seq.get();
        }
    }

    private static class MemIdempotencyRepository implements IdempotencyRepository {
        private final HashSet<String> keys = new HashSet<>();

        @Override
        public boolean insertIfAbsent(String key, Instant expiresAt) {
            return keys.add(key);
        }

        @Override
        public boolean exists(String key) {
            return keys.contains(key);
        }
    }
}
