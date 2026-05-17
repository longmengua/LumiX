/*
 * 檔案用途：應用服務，編排領域模型、Repository 與外部基礎設施完成業務流程。
 */
package com.example.exchange.application.service;

import com.example.exchange.application.command.PlaceOrderCommand;
import com.example.exchange.application.command.AmendOrderCommand;
import com.example.exchange.application.command.CancelReplaceOrderCommand;
import com.example.exchange.application.usecase.AmendOrderUseCase;
import com.example.exchange.application.usecase.CancelOrderUseCase;
import com.example.exchange.application.usecase.CancelReplaceOrderUseCase;
import com.example.exchange.application.usecase.PlaceOrderUseCase;
import com.example.exchange.domain.event.OrderLifecycleEvent;
import com.example.exchange.domain.event.TradeExecuted;
import com.example.exchange.domain.model.entity.Account;
import com.example.exchange.domain.model.entity.Order;
import com.example.exchange.domain.model.entity.Position;
import com.example.exchange.domain.model.entity.Symbol;
import com.example.exchange.domain.model.entity.SymbolConfig;
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
import com.example.exchange.infra.config.RiskControlsProperties;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderAccountingIntegrationTest {

    @Test
    void fillUpdatesPositionsFeesLedgerAndMarketData() {
        MemAccountRepository accountRepo = new MemAccountRepository();
        MemWalletLedgerRepository ledgerRepo = new MemWalletLedgerRepository();
        MemPositionRepository positionRepo = new MemPositionRepository();
        MemOrderRepository orderRepo = new MemOrderRepository();
        MemEventStore eventStore = new MemEventStore();
        List<Object> published = new ArrayList<>();

        DefaultSymbolConfigRepository symbolRepo = new DefaultSymbolConfigRepository();
        InMemoryMatchingEngine matchingEngine = new InMemoryMatchingEngine();
        WalletLedgerService walletLedgerService = new WalletLedgerService(accountRepo, ledgerRepo);
        RiskService riskService = new RiskService(accountRepo, positionRepo, orderRepo, matchingEngine, walletLedgerService, riskControls());
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
        PlaceOrderUseCase placeOrderUseCase = new PlaceOrderUseCase(orderService, riskService, symbolRepo, published::add);

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
        List<TradeExecuted> publishedTrades = published.stream()
                .filter(TradeExecuted.class::isInstance)
                .map(TradeExecuted.class::cast)
                .toList();
        List<OrderLifecycleEvent> orderEvents = published.stream()
                .filter(OrderLifecycleEvent.class::isInstance)
                .map(OrderLifecycleEvent.class::cast)
                .toList();
        assertThat(publishedTrades).hasSize(2);
        assertThat(orderEvents).extracting(OrderLifecycleEvent::stage)
                .contains(
                        OrderLifecycleEvent.Stage.CREATED,
                        OrderLifecycleEvent.Stage.ACCEPTED,
                        OrderLifecycleEvent.Stage.UPDATED,
                        OrderLifecycleEvent.Stage.FILLED
                );
        assertThat(marketDataService.ticker("BTCUSDT")).isPresent();
        assertThat(marketDataService.trades("BTCUSDT", 10)).hasSize(1);

        matchingEngine.shutdown();
    }

    @Test
    void rejectedPreCheckPublishesOrderLifecycleEvent() {
        MemAccountRepository accountRepo = new MemAccountRepository();
        MemWalletLedgerRepository ledgerRepo = new MemWalletLedgerRepository();
        MemPositionRepository positionRepo = new MemPositionRepository();
        MemOrderRepository orderRepo = new MemOrderRepository();
        MemEventStore eventStore = new MemEventStore();
        List<Object> published = new ArrayList<>();

        DefaultSymbolConfigRepository symbolRepo = new DefaultSymbolConfigRepository();
        InMemoryMatchingEngine matchingEngine = new InMemoryMatchingEngine();
        WalletLedgerService walletLedgerService = new WalletLedgerService(accountRepo, ledgerRepo);
        RiskService riskService = new RiskService(accountRepo, positionRepo, orderRepo, matchingEngine, walletLedgerService, riskControls());
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
        PlaceOrderUseCase placeOrderUseCase = new PlaceOrderUseCase(orderService, riskService, symbolRepo, published::add);

        assertThatThrownBy(() -> placeOrderUseCase.handle(command(9, OrderSide.BUY, "100.00", "1.000")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("insufficient available balance");

        List<OrderLifecycleEvent> orderEvents = published.stream()
                .filter(OrderLifecycleEvent.class::isInstance)
                .map(OrderLifecycleEvent.class::cast)
                .toList();
        assertThat(orderEvents).extracting(OrderLifecycleEvent::stage)
                .containsExactly(OrderLifecycleEvent.Stage.CREATED, OrderLifecycleEvent.Stage.REJECTED);
        assertThat(orderEvents.getLast().reasonCode()).isEqualTo("INSUFFICIENT_BALANCE");
        assertThat(orderRepo.findAllOrders(9L, "BTCUSDT")).isEmpty();

        matchingEngine.shutdown();
    }

    @Test
    void maxOpenOrdersPreCheckRejectsNewOrder() {
        MemAccountRepository accountRepo = new MemAccountRepository();
        MemWalletLedgerRepository ledgerRepo = new MemWalletLedgerRepository();
        MemPositionRepository positionRepo = new MemPositionRepository();
        MemOrderRepository orderRepo = new MemOrderRepository();
        MemEventStore eventStore = new MemEventStore();
        List<Object> published = new ArrayList<>();

        Symbol symbol = Symbol.builder().base("BTC").quote("USDT").priceScale(2).qtyScale(3).build();
        var symbolConfig = btcConfigWithMaxOpenOrders(1);
        InMemoryMatchingEngine matchingEngine = new InMemoryMatchingEngine();
        WalletLedgerService walletLedgerService = new WalletLedgerService(accountRepo, ledgerRepo);
        RiskService riskService = new RiskService(accountRepo, positionRepo, orderRepo, matchingEngine, walletLedgerService, riskControls());
        MarketDataService marketDataService = new MarketDataService();
        IdempotencyService idempotencyService = new IdempotencyService(new MemIdempotencyRepository());
        OrderService orderService = new OrderService(
                matchingEngine,
                positionRepo,
                eventStore,
                published::add,
                orderRepo,
                raw -> "BTCUSDT".equalsIgnoreCase(raw) ? Optional.of(symbolConfig) : Optional.empty(),
                walletLedgerService,
                new FeeService(),
                riskService,
                marketDataService,
                idempotencyService
        );
        PlaceOrderUseCase placeOrderUseCase = new PlaceOrderUseCase(
                orderService,
                riskService,
                raw -> "BTCUSDT".equalsIgnoreCase(raw) ? Optional.of(symbolConfig) : Optional.empty(),
                published::add
        );

        orderRepo.save(Order.builder()
                .uid(3)
                .symbol(symbol)
                .side(OrderSide.BUY)
                .type(OrderType.LIMIT)
                .price(new BigDecimal("100.00"))
                .qty(new BigDecimal("1.000"))
                .origQty(new BigDecimal("1.000"))
                .build());

        assertThatThrownBy(() -> placeOrderUseCase.handle(command(3, OrderSide.BUY, "101.00", "1.000")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("max open orders exceeded");

        List<OrderLifecycleEvent> orderEvents = published.stream()
                .filter(OrderLifecycleEvent.class::isInstance)
                .map(OrderLifecycleEvent.class::cast)
                .toList();
        assertThat(orderEvents).extracting(OrderLifecycleEvent::stage)
                .containsExactly(OrderLifecycleEvent.Stage.CREATED, OrderLifecycleEvent.Stage.REJECTED);
        assertThat(orderEvents.getLast().reasonCode()).isEqualTo("MAX_OPEN_ORDERS");
        assertThat(orderRepo.findAllOrders(3L, "BTCUSDT")).hasSize(1);
        assertThat(ledgerRepo.findByUid(3)).isEmpty();

        matchingEngine.shutdown();
    }

    @Test
    void duplicateClientOrderIdPreCheckRejectsNewOrder() {
        MemAccountRepository accountRepo = new MemAccountRepository();
        MemWalletLedgerRepository ledgerRepo = new MemWalletLedgerRepository();
        MemPositionRepository positionRepo = new MemPositionRepository();
        MemOrderRepository orderRepo = new MemOrderRepository();

        SymbolConfig symbolConfig = btcConfigWithMaxOpenOrders(10);
        Symbol symbol = symbolConfig.toSymbol();
        InMemoryMatchingEngine matchingEngine = new InMemoryMatchingEngine();
        WalletLedgerService walletLedgerService = new WalletLedgerService(accountRepo, ledgerRepo);
        RiskService riskService = new RiskService(accountRepo, positionRepo, orderRepo, matchingEngine, walletLedgerService, riskControls());

        orderRepo.save(Order.builder()
                .uid(4)
                .symbol(symbol)
                .side(OrderSide.BUY)
                .type(OrderType.LIMIT)
                .price(new BigDecimal("100.00"))
                .qty(new BigDecimal("1.000"))
                .origQty(new BigDecimal("1.000"))
                .clientOrderId("strategy-4-1")
                .build());

        Order incoming = Order.builder()
                .uid(4)
                .symbol(symbol)
                .side(OrderSide.SELL)
                .type(OrderType.LIMIT)
                .price(new BigDecimal("101.00"))
                .qty(new BigDecimal("1.000"))
                .origQty(new BigDecimal("1.000"))
                .clientOrderId(" strategy-4-1 ")
                .build();

        assertThatThrownBy(() -> riskService.preCheckAndReserve(incoming, symbolConfig))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("duplicate clientOrderId");
        assertThat(incoming.getStatus()).isEqualTo(Order.Status.REJECTED);
        assertThat(incoming.getRejectCode()).isEqualTo("DUPLICATE_CLIENT_ORDER_ID");
        assertThat(ledgerRepo.findByUid(4)).isEmpty();

        matchingEngine.shutdown();
    }

    @Test
    void riskSwitchesRejectOrderEntryReduceOnlyAndSuspendedSymbol() {
        SymbolConfig symbolConfig = btcConfigWithMaxOpenOrders(10);

        RiskControlsProperties orderEntryHalt = riskControls();
        orderEntryHalt.setOrderEntryHalt(true);
        Order haltedOrder = limitOrder(5, symbolConfig.toSymbol(), OrderSide.BUY, "100.00", false);
        assertRiskSwitchRejects(haltedOrder, symbolConfig, orderEntryHalt, "ORDER_ENTRY_HALTED");

        RiskControlsProperties reduceOnlyMode = riskControls();
        reduceOnlyMode.setReduceOnlyMode(true);
        Order nonReduceOnlyOrder = limitOrder(5, symbolConfig.toSymbol(), OrderSide.BUY, "100.00", false);
        assertRiskSwitchRejects(nonReduceOnlyOrder, symbolConfig, reduceOnlyMode, "GLOBAL_REDUCE_ONLY");

        RiskControlsProperties suspendedSymbol = riskControls();
        suspendedSymbol.setSuspendedSymbols(List.of(" btcusdt "));
        Order suspendedOrder = limitOrder(5, symbolConfig.toSymbol(), OrderSide.BUY, "100.00", true);
        assertRiskSwitchRejects(suspendedOrder, symbolConfig, suspendedSymbol, "SYMBOL_SUSPENDED");
    }

    @Test
    void bulkCancelOpenOrdersReleasesReserveAndPublishesLifecycleEvents() {
        MemAccountRepository accountRepo = new MemAccountRepository();
        MemWalletLedgerRepository ledgerRepo = new MemWalletLedgerRepository();
        MemPositionRepository positionRepo = new MemPositionRepository();
        MemOrderRepository orderRepo = new MemOrderRepository();
        MemEventStore eventStore = new MemEventStore();
        List<Object> published = new ArrayList<>();

        DefaultSymbolConfigRepository symbolRepo = new DefaultSymbolConfigRepository();
        InMemoryMatchingEngine matchingEngine = new InMemoryMatchingEngine();
        WalletLedgerService walletLedgerService = new WalletLedgerService(accountRepo, ledgerRepo);
        RiskService riskService = new RiskService(accountRepo, positionRepo, orderRepo, matchingEngine, walletLedgerService, riskControls());
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
        PlaceOrderUseCase placeOrderUseCase = new PlaceOrderUseCase(orderService, riskService, symbolRepo, published::add);
        CancelOrderUseCase cancelOrderUseCase = new CancelOrderUseCase(
                orderRepo,
                symbolRepo,
                matchingEngine,
                walletLedgerService,
                marketDataService,
                published::add
        );

        walletLedgerService.deposit(11, "USDT", new BigDecimal("1000"), "deposit-11");
        placeOrderUseCase.handle(command(11, OrderSide.BUY, "100.00", "1.000"));
        Account accountAfterPlace = accountRepo.findByUid(11).orElseThrow();
        assertThat(accountAfterPlace.crossOrderHold()).isGreaterThan(BigDecimal.ZERO);

        int canceled = cancelOrderUseCase.cancelOpenOrders(11, "BTCUSDT");

        assertThat(canceled).isEqualTo(1);
        assertThat(accountRepo.findByUid(11).orElseThrow().crossOrderHold()).isEqualByComparingTo("0");
        assertThat(accountRepo.findByUid(11).orElseThrow().crossAvailable()).isEqualByComparingTo("1000");
        assertThat(orderRepo.findAllOrders(11L, "BTCUSDT")).singleElement()
                .extracting(Order::getStatus)
                .isEqualTo(Order.Status.CANCELED);
        assertThat(published.stream()
                .filter(OrderLifecycleEvent.class::isInstance)
                .map(OrderLifecycleEvent.class::cast)
                .map(OrderLifecycleEvent::stage)
                .toList()).contains(OrderLifecycleEvent.Stage.CANCELED);

        matchingEngine.shutdown();
    }

    @Test
    void amendOrderUpdatesBookReserveAndLifecycleEvent() {
        MemAccountRepository accountRepo = new MemAccountRepository();
        MemWalletLedgerRepository ledgerRepo = new MemWalletLedgerRepository();
        MemPositionRepository positionRepo = new MemPositionRepository();
        MemOrderRepository orderRepo = new MemOrderRepository();
        MemEventStore eventStore = new MemEventStore();
        List<Object> published = new ArrayList<>();

        DefaultSymbolConfigRepository symbolRepo = new DefaultSymbolConfigRepository();
        InMemoryMatchingEngine matchingEngine = new InMemoryMatchingEngine();
        WalletLedgerService walletLedgerService = new WalletLedgerService(accountRepo, ledgerRepo);
        RiskService riskService = new RiskService(accountRepo, positionRepo, orderRepo, matchingEngine, walletLedgerService, riskControls());
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
        PlaceOrderUseCase placeOrderUseCase = new PlaceOrderUseCase(orderService, riskService, symbolRepo, published::add);
        AmendOrderUseCase amendOrderUseCase = new AmendOrderUseCase(
                orderRepo,
                symbolRepo,
                matchingEngine,
                riskService,
                marketDataService,
                published::add
        );

        walletLedgerService.deposit(21, "USDT", new BigDecimal("1000"), "deposit-21");
        Order order = placeOrderUseCase.place(command(21, OrderSide.BUY, "100.00", "1.000"));
        BigDecimal initialReserve = order.getReservedAmount();

        amendOrderUseCase.handle(new AmendOrderCommand(
                order.getId(),
                21,
                new BigDecimal("99.00"),
                new BigDecimal("0.500"),
                "amended-21"
        ));

        Order amended = orderRepo.findById(order.getId()).orElseThrow();
        assertThat(amended.getStatus()).isEqualTo(Order.Status.NEW);
        assertThat(amended.getPrice()).isEqualByComparingTo("99.00");
        assertThat(amended.getQty()).isEqualByComparingTo("0.500");
        assertThat(amended.getOrigQty()).isEqualByComparingTo("0.500");
        assertThat(amended.getClientOrderId()).isEqualTo("amended-21");
        assertThat(amended.getReservedAmount()).isLessThan(initialReserve);
        assertThat(accountRepo.findByUid(21).orElseThrow().crossOrderHold())
                .isEqualByComparingTo(amended.getReservedAmount());
        assertThat(matchingEngine.top("BTCUSDT").orElseThrow().getBestBid())
                .isEqualByComparingTo("99.00");
        assertThat(published.stream()
                .filter(OrderLifecycleEvent.class::isInstance)
                .map(OrderLifecycleEvent.class::cast)
                .map(OrderLifecycleEvent::stage)
                .toList()).contains(OrderLifecycleEvent.Stage.UPDATED);

        matchingEngine.shutdown();
    }

    @Test
    void cancelReplaceCancelsOriginalAndPlacesReplacement() {
        MemAccountRepository accountRepo = new MemAccountRepository();
        MemWalletLedgerRepository ledgerRepo = new MemWalletLedgerRepository();
        MemPositionRepository positionRepo = new MemPositionRepository();
        MemOrderRepository orderRepo = new MemOrderRepository();
        MemEventStore eventStore = new MemEventStore();
        List<Object> published = new ArrayList<>();

        DefaultSymbolConfigRepository symbolRepo = new DefaultSymbolConfigRepository();
        InMemoryMatchingEngine matchingEngine = new InMemoryMatchingEngine();
        WalletLedgerService walletLedgerService = new WalletLedgerService(accountRepo, ledgerRepo);
        RiskService riskService = new RiskService(accountRepo, positionRepo, orderRepo, matchingEngine, walletLedgerService, riskControls());
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
        PlaceOrderUseCase placeOrderUseCase = new PlaceOrderUseCase(orderService, riskService, symbolRepo, published::add);
        CancelOrderUseCase cancelOrderUseCase = new CancelOrderUseCase(
                orderRepo,
                symbolRepo,
                matchingEngine,
                walletLedgerService,
                marketDataService,
                published::add
        );
        CancelReplaceOrderUseCase cancelReplaceOrderUseCase = new CancelReplaceOrderUseCase(
                orderRepo,
                cancelOrderUseCase,
                placeOrderUseCase
        );

        walletLedgerService.deposit(22, "USDT", new BigDecimal("1000"), "deposit-22");
        Order original = placeOrderUseCase.place(command(22, OrderSide.BUY, "100.00", "1.000"));

        cancelReplaceOrderUseCase.handle(new CancelReplaceOrderCommand(
                original.getId(),
                22,
                new BigDecimal("101.00"),
                new BigDecimal("0.750"),
                "replace-22"
        ));

        List<Order> orders = orderRepo.findAllOrders(22L, "BTCUSDT");
        assertThat(orders).hasSize(2);
        assertThat(orderRepo.findById(original.getId()).orElseThrow().getStatus()).isEqualTo(Order.Status.CANCELED);
        Order replacement = orders.stream()
                .filter(order -> !order.getId().equals(original.getId()))
                .findFirst()
                .orElseThrow();
        assertThat(replacement.getStatus()).isEqualTo(Order.Status.NEW);
        assertThat(replacement.getPrice()).isEqualByComparingTo("101.00");
        assertThat(replacement.getQty()).isEqualByComparingTo("0.750");
        assertThat(replacement.getClientOrderId()).isEqualTo("replace-22");
        assertThat(accountRepo.findByUid(22).orElseThrow().crossOrderHold())
                .isEqualByComparingTo(replacement.getReservedAmount());
        assertThat(published.stream()
                .filter(OrderLifecycleEvent.class::isInstance)
                .map(OrderLifecycleEvent.class::cast)
                .map(OrderLifecycleEvent::stage)
                .toList()).contains(OrderLifecycleEvent.Stage.CANCELED, OrderLifecycleEvent.Stage.CREATED);

        matchingEngine.shutdown();
    }

    @Test
    void cancelOnDisconnectCancelsRegisteredOpenOrders() {
        MemAccountRepository accountRepo = new MemAccountRepository();
        MemWalletLedgerRepository ledgerRepo = new MemWalletLedgerRepository();
        MemPositionRepository positionRepo = new MemPositionRepository();
        MemOrderRepository orderRepo = new MemOrderRepository();
        MemEventStore eventStore = new MemEventStore();
        List<Object> published = new ArrayList<>();

        DefaultSymbolConfigRepository symbolRepo = new DefaultSymbolConfigRepository();
        InMemoryMatchingEngine matchingEngine = new InMemoryMatchingEngine();
        WalletLedgerService walletLedgerService = new WalletLedgerService(accountRepo, ledgerRepo);
        RiskService riskService = new RiskService(accountRepo, positionRepo, orderRepo, matchingEngine, walletLedgerService, riskControls());
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
        PlaceOrderUseCase placeOrderUseCase = new PlaceOrderUseCase(orderService, riskService, symbolRepo, published::add);
        CancelOrderUseCase cancelOrderUseCase = new CancelOrderUseCase(
                orderRepo,
                symbolRepo,
                matchingEngine,
                walletLedgerService,
                marketDataService,
                published::add
        );
        CancelOnDisconnectService cancelOnDisconnectService = new CancelOnDisconnectService(cancelOrderUseCase);

        walletLedgerService.deposit(31, "USDT", new BigDecimal("1000"), "deposit-31");
        placeOrderUseCase.place(command(31, OrderSide.BUY, "100.00", "1.000"));
        cancelOnDisconnectService.register("ws-31", 31, "btcusdt");

        int canceled = cancelOnDisconnectService.cancelForConnection("ws-31");

        assertThat(canceled).isEqualTo(1);
        assertThat(cancelOnDisconnectService.registeredCount()).isZero();
        assertThat(orderRepo.findAllOrders(31L, "BTCUSDT")).singleElement()
                .extracting(Order::getStatus)
                .isEqualTo(Order.Status.CANCELED);
        assertThat(accountRepo.findByUid(31).orElseThrow().crossOrderHold()).isEqualByComparingTo("0");

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

    private static SymbolConfig btcConfigWithMaxOpenOrders(int maxOpenOrders) {
        return SymbolConfig.builder()
                .symbol("BTCUSDT")
                .baseAsset("BTC")
                .quoteAsset("USDT")
                .priceTick(new BigDecimal("0.01"))
                .lotSize(new BigDecimal("0.001"))
                .minQty(new BigDecimal("0.001"))
                .minNotional(new BigDecimal("5"))
                .maxOrderNotional(new BigDecimal("1000000"))
                .maxPositionNotional(new BigDecimal("5000000"))
                .maxOpenOrders(maxOpenOrders)
                .maxLeverage(125)
                .makerFeeRate(new BigDecimal("0.0002"))
                .takerFeeRate(new BigDecimal("0.0005"))
                .makerRebateRate(BigDecimal.ZERO)
                .referralRebateRate(new BigDecimal("0.00005"))
                .priceBandRate(new BigDecimal("0.10"))
                .maintenanceMarginRate(new BigDecimal("0.005"))
                .tradingEnabled(true)
                .build();
    }

    private static RiskControlsProperties riskControls() {
        return new RiskControlsProperties();
    }

    private static Order limitOrder(long uid, Symbol symbol, OrderSide side, String price, boolean reduceOnly) {
        return Order.builder()
                .uid(uid)
                .symbol(symbol)
                .side(side)
                .type(OrderType.LIMIT)
                .price(new BigDecimal(price))
                .qty(new BigDecimal("1.000"))
                .origQty(new BigDecimal("1.000"))
                .reduceOnly(reduceOnly)
                .build();
    }

    private static void assertRiskSwitchRejects(
            Order order,
            SymbolConfig symbolConfig,
            RiskControlsProperties riskControlsProperties,
            String rejectCode
    ) {
        MemAccountRepository accountRepo = new MemAccountRepository();
        MemWalletLedgerRepository ledgerRepo = new MemWalletLedgerRepository();
        MemPositionRepository positionRepo = new MemPositionRepository();
        MemOrderRepository orderRepo = new MemOrderRepository();
        InMemoryMatchingEngine matchingEngine = new InMemoryMatchingEngine();
        WalletLedgerService walletLedgerService = new WalletLedgerService(accountRepo, ledgerRepo);
        RiskService riskService = new RiskService(
                accountRepo,
                positionRepo,
                orderRepo,
                matchingEngine,
                walletLedgerService,
                riskControlsProperties
        );

        assertThatThrownBy(() -> riskService.preCheckAndReserve(order, symbolConfig))
                .isInstanceOf(IllegalStateException.class);
        assertThat(order.getStatus()).isEqualTo(Order.Status.REJECTED);
        assertThat(order.getRejectCode()).isEqualTo(rejectCode);
        assertThat(ledgerRepo.findByUid(order.getUid())).isEmpty();

        matchingEngine.shutdown();
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
