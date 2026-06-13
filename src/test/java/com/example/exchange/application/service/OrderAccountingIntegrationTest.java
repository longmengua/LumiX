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
import com.example.exchange.domain.model.enums.MatchingCommandType;
import com.example.exchange.domain.model.enums.OrderSide;
import com.example.exchange.domain.model.enums.OrderType;
import com.example.exchange.domain.model.dto.MatchingSequencerLease;
import com.example.exchange.domain.repository.AccountRepository;
import com.example.exchange.domain.repository.EventStore;
import com.example.exchange.domain.repository.IdempotencyRepository;
import com.example.exchange.domain.repository.OrderRepository;
import com.example.exchange.domain.repository.PositionRepository;
import com.example.exchange.domain.repository.MatchingSequencerLeaseStore;
import com.example.exchange.domain.repository.WalletLedgerRepository;
import com.example.exchange.infra.config.DefaultSymbolConfigRepository;
import com.example.exchange.infra.config.RiskControlsProperties;
import com.example.exchange.infra.matching.InMemoryMatchingCommandLog;
import com.example.exchange.infra.matching.InMemoryMatchingEngine;
import com.example.exchange.interfaces.web.exception.BusinessErrorCode;
import com.example.exchange.interfaces.web.exception.BusinessException;
import com.example.exchange.infra.matching.InMemoryMatchingEventLog;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 訂單、撮合、帳務、持倉、market data 與 lifecycle event 的整合測試。
 *
 * <p>這個 test class 使用 in-memory repository stub，把下單流程從 UseCase
 * 跑到 MatchingEngine / RiskService / WalletLedgerService，確認核心 MVP 行為
 * 在不啟動 Redis、Kafka、DB 的情況下仍可驗證。</p>
 */
class OrderAccountingIntegrationTest {

    @Test
    @DisplayName("成交後更新雙方持倉、費用、ledger、market data 與 lifecycle event")
    /**
     * 流程：建立完整下單服務鏈路 -> 賣單先掛簿、買單吃單 -> 驗證 position、fee、ledger、market data 與事件。
     */
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

        // 先掛賣再掛買，讓第二筆訂單主動吃掉第一筆掛單。
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
    @DisplayName("費率調整後舊掛單沿用下單快照，新訂單使用新費率")
    /**
     * 情境：maker 先用舊費率掛單，後台調高 symbol fee，再由 taker 吃單；舊 maker 不應被新費率 retroactively 影響。
     */
    void feeChangeDoesNotAffectExistingRestingOrderSnapshots() {
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

        // Seller rests on the book while BTCUSDT maker/taker rates are still 0.0002 / 0.0005.
        Order restingMaker = placeOrderUseCase.place(command(2, OrderSide.SELL, "100.00", "1.000"));
        SymbolConfig config = symbolRepo.findBySymbol("BTCUSDT").orElseThrow();
        config.setMakerFeeRate(new BigDecimal("0.0010"));
        config.setTakerFeeRate(new BigDecimal("0.0020"));
        symbolRepo.save(config);

        // Buyer enters after the fee change, so the taker fee snapshot should use the new 0.0020 rate.
        Order incomingTaker = placeOrderUseCase.place(command(1, OrderSide.BUY, "100.00", "1.000"));

        Symbol symbol = Symbol.builder().base("BTC").quote("USDT").priceScale(1).qtyScale(3).build();
        Position buyerPosition = positionRepo.find(1, symbol).orElseThrow();
        Position sellerPosition = positionRepo.find(2, symbol).orElseThrow();

        assertThat(restingMaker.getMakerFeeRateSnapshot()).isEqualByComparingTo("0.0002");
        assertThat(restingMaker.getTakerFeeRateSnapshot()).isEqualByComparingTo("0.0005");
        assertThat(incomingTaker.getMakerFeeRateSnapshot()).isEqualByComparingTo("0.0010");
        assertThat(incomingTaker.getTakerFeeRateSnapshot()).isEqualByComparingTo("0.0020");
        assertThat(sellerPosition.getFeePaid()).isEqualByComparingTo("0.020000000000000000");
        assertThat(buyerPosition.getFeePaid()).isEqualByComparingTo("0.200000000000000000");

        matchingEngine.shutdown();
    }

    @Test
    @DisplayName("風控預檢拒單時發布 CREATED 和 REJECTED lifecycle event")
    /**
     * 流程：不給帳戶入金 -> 直接下買單觸發 balance pre-check -> 驗證 CREATED 後接 REJECTED 且不落訂單。
     */
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
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getErrorCode())
                .isEqualTo(BusinessErrorCode.ORDER_INSUFFICIENT_BALANCE);

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
    @DisplayName("超過 max open orders 時拒絕新單且不寫 ledger")
    /**
     * 流程：symbol 設 max open orders = 1 -> 先放一筆 open order -> 第二筆下單被拒且沒有 ledger side effect。
     */
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
    @DisplayName("重複 clientOrderId 會被 pre-trade risk check 拒絕")
    /**
     * 流程：repository 已有同 uid/clientOrderId 的 open order -> incoming trim 後相同 -> pre-check 拒絕重複 id。
     */
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
    @DisplayName("全站風控開關會拒絕下單、非 reduce-only 單與停牌 symbol")
    /**
     * 流程：分別開啟 order-entry halt、reduce-only mode、symbol suspension -> 共用 helper 驗證拒單 code。
     */
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
    @DisplayName("pre-trade frequency limit 會拒絕同 uid/symbol 視窗內過量下單")
    /**
     * 流程：啟用 uid+symbol 固定視窗每分鐘最多 1 筆新單 -> 第一筆預檢成功 -> 第二筆同 uid/symbol 被拒。
     * 期望：第二筆在 reserve 前被擋下，避免短時間濫送訂單消耗帳務與撮合資源。
     */
    void orderEntryFrequencyLimitRejectsBurstOrdersBeforeReserve() {
        MemAccountRepository accountRepo = new MemAccountRepository();
        MemWalletLedgerRepository ledgerRepo = new MemWalletLedgerRepository();
        MemPositionRepository positionRepo = new MemPositionRepository();
        MemOrderRepository orderRepo = new MemOrderRepository();
        InMemoryMatchingEngine matchingEngine = new InMemoryMatchingEngine();
        WalletLedgerService walletLedgerService = new WalletLedgerService(accountRepo, ledgerRepo);
        RiskControlsProperties riskControls = riskControls();
        riskControls.getOrderEntryFrequencyLimit().setEnabled(true);
        riskControls.getOrderEntryFrequencyLimit().setMaxOrders(1);
        riskControls.getOrderEntryFrequencyLimit().setWindowSeconds(60);
        RiskService riskService = new RiskService(
                accountRepo,
                positionRepo,
                orderRepo,
                matchingEngine,
                walletLedgerService,
                riskControls
        );
        SymbolConfig symbolConfig = btcConfigWithMaxOpenOrders(10);
        Account account = new Account(51);
        account.deposit(new BigDecimal("10000"));
        accountRepo.save(account);

        Order first = limitOrder(51, symbolConfig.toSymbol(), OrderSide.BUY, "100.00", false);
        riskService.preCheckAndReserve(first, symbolConfig);
        Order burst = limitOrder(51, symbolConfig.toSymbol(), OrderSide.BUY, "101.00", false);

        assertThatThrownBy(() -> riskService.preCheckAndReserve(burst, symbolConfig))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("order entry frequency limit exceeded");
        assertThat(burst.getRejectCode()).isEqualTo("ORDER_ENTRY_FREQUENCY_LIMIT");
        assertThat(ledgerRepo.findByUid(51))
                .extracting(WalletLedgerEntry::getReason)
                .containsExactly("order_reserve");

        matchingEngine.shutdown();
    }

    @Test
    @DisplayName("risk tiers 會套用初始保證金率、槓桿上限與階梯倉位上限")
    /**
     * 流程：建立兩階 risk tiers -> 送入 tier2 名義金額 -> 驗證 reserve 使用 tier2 初始保證金率，並拒絕超槓桿與超階梯上限。
     */
    void riskTiersApplyInitialMarginLeverageAndSteppedPositionLimit() {
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
                riskControls()
        );
        SymbolConfig symbolConfig = btcConfigWithRiskTiers();
        Account account = new Account(61);
        account.deposit(new BigDecimal("10000"));
        accountRepo.save(account);

        Order tierTwoOrder = limitOrder(61, symbolConfig.toSymbol(), OrderSide.BUY, "100.00", "15.000", 5);
        riskService.preCheckAndReserve(tierTwoOrder, symbolConfig);

        assertThat(tierTwoOrder.getReservedAmount()).isEqualByComparingTo("300.750000000000000000");
        assertThat(accountRepo.findByUid(61).orElseThrow().crossOrderHold())
                .isEqualByComparingTo("300.750000000000000000");

        Order overLeverage = limitOrder(61, symbolConfig.toSymbol(), OrderSide.BUY, "100.00", "15.000", 10);
        assertThatThrownBy(() -> riskService.preCheckAndReserve(overLeverage, symbolConfig))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("leverage exceeds symbol limit");
        assertThat(overLeverage.getRejectCode()).isEqualTo("INVALID_LEVERAGE");

        Order overLimit = limitOrder(61, symbolConfig.toSymbol(), OrderSide.BUY, "100.00", "25.000", 5);
        assertThatThrownBy(() -> riskService.preCheckAndReserve(overLimit, symbolConfig))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("position notional exceeds risk limit");
        assertThat(overLimit.getRejectCode()).isEqualTo("MAX_POSITION_NOTIONAL");

        matchingEngine.shutdown();
    }

    @Test
    @DisplayName("批量撤單會釋放 reserve 並發布取消事件")
    /**
     * 流程：入金後掛買單產生 reserve -> cancelOpenOrders -> 驗證 hold 釋放、訂單取消、事件發布。
     */
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
    @DisplayName("改單會更新 order book、reserve 與 UPDATED lifecycle event")
    /**
     * 流程：先掛買單並記錄 reserve -> amend 降價降量 -> 驗證 order、帳戶 hold、book top 與 UPDATED event。
     */
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

        // 將買單價格調低、剩餘量縮小，預期 reserve 也同步下降。
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
    @DisplayName("cancel-replace 會取消原單並建立 replacement order")
    /**
     * 流程：先掛原買單 -> cancelReplace 取消原單再建立新單 -> 驗證兩筆訂單狀態、reserve 與 lifecycle events。
     */
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
    @DisplayName("worker ready 時 cancel-replace 會經由 fenced cancel + submit 完成帳務安全流程")
    /**
     * 流程：建立 ready worker owner context -> 原單與 cancel-replace 都走 worker execution ->
     * 驗證 command log 保留 owner/epoch，且帳務仍是釋放原 reserve 後預凍 replacement。
     */
    void workerReadyCancelReplaceUsesFencedCancelAndSubmitAccountingFlow() {
        MemAccountRepository accountRepo = new MemAccountRepository();
        MemWalletLedgerRepository ledgerRepo = new MemWalletLedgerRepository();
        MemPositionRepository positionRepo = new MemPositionRepository();
        MemOrderRepository orderRepo = new MemOrderRepository();
        MemEventStore eventStore = new MemEventStore();
        List<Object> published = new ArrayList<>();

        DefaultSymbolConfigRepository symbolRepo = new DefaultSymbolConfigRepository();
        InMemoryMatchingCommandLog commandLog = new InMemoryMatchingCommandLog();
        InMemoryMatchingEventLog eventLog = new InMemoryMatchingEventLog();
        InMemoryMatchingEngine matchingEngine = new InMemoryMatchingEngine(commandLog, eventLog);
        MatchingSequencerLeaseService leaseService =
                new MatchingSequencerLeaseService(new TestLeaseStore(), new MutableClock(Instant.parse("2026-05-29T00:00:00Z")));
        MatchingSequencerLease lease = leaseService.acquire("BTCUSDT", "worker-a", Duration.ofSeconds(30)).orElseThrow();
        MatchingWorkerCommandRouter router = new MatchingWorkerCommandRouter(leaseService, commandLog, eventLog);
        MatchingWorkerExecutionService workerExecution = new MatchingWorkerExecutionService(router, matchingEngine);
        MatchingWorkerLifecycleService workerLifecycle = readyWorkerLifecycle("BTCUSDT", lease);

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
        orderService.setMatchingWorkerLifecycleService(workerLifecycle);
        orderService.setMatchingWorkerExecutionService(workerExecution);
        PlaceOrderUseCase placeOrderUseCase = new PlaceOrderUseCase(orderService, riskService, symbolRepo, published::add);
        CancelOrderUseCase cancelOrderUseCase = new CancelOrderUseCase(
                orderRepo,
                symbolRepo,
                matchingEngine,
                walletLedgerService,
                marketDataService,
                published::add
        );
        cancelOrderUseCase.setMatchingWorkerLifecycleService(workerLifecycle);
        cancelOrderUseCase.setMatchingWorkerExecutionService(workerExecution);
        CancelReplaceOrderUseCase cancelReplaceOrderUseCase = new CancelReplaceOrderUseCase(
                orderRepo,
                cancelOrderUseCase,
                placeOrderUseCase
        );

        walletLedgerService.deposit(23, "USDT", new BigDecimal("1000"), "deposit-23");
        Order original = placeOrderUseCase.place(command(23, OrderSide.BUY, "100.00", "1.000"));

        cancelReplaceOrderUseCase.handle(new CancelReplaceOrderCommand(
                original.getId(),
                23,
                new BigDecimal("101.00"),
                new BigDecimal("0.750"),
                "replace-worker-23"
        ));

        assertThat(commandLog.listAll("BTCUSDT"))
                .extracting(entry -> entry.type())
                .containsExactly(MatchingCommandType.SUBMIT, MatchingCommandType.CANCEL, MatchingCommandType.SUBMIT);
        assertThat(commandLog.listAll("BTCUSDT"))
                .allSatisfy(entry -> {
                    assertThat(entry.ownerId()).isEqualTo("worker-a");
                    assertThat(entry.ownerEpoch()).isEqualTo(lease.epoch());
                });
        Order replacement = orderRepo.findAllOrders(23L, "BTCUSDT").stream()
                .filter(order -> !order.getId().equals(original.getId()))
                .findFirst()
                .orElseThrow();
        assertThat(orderRepo.findById(original.getId()).orElseThrow().getStatus()).isEqualTo(Order.Status.CANCELED);
        assertThat(replacement.getStatus()).isEqualTo(Order.Status.NEW);
        assertThat(accountRepo.findByUid(23).orElseThrow().crossOrderHold())
                .isEqualByComparingTo(replacement.getReservedAmount());

        matchingEngine.shutdown();
    }

    @Test
    @DisplayName("cancel-on-disconnect 會撤掉該 WebSocket 註冊範圍內的 open orders")
    /**
     * 流程：掛單後註冊 connection/symbol -> 模擬連線斷開 -> 驗證註冊清除、訂單取消與 reserve 釋放。
     */
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

    @Test
    @DisplayName("cancel-on-disconnect 重連轉移後舊連線關閉不會誤撤單")
    /**
     * 流程：掛單後註冊舊 connection -> 新 connection resume 舊註冊 ->
     * 舊 connection close event 晚到時不撤單，新 connection close 才真正撤單。
     */
    void cancelOnDisconnectResumeTransfersRegistrationToNewConnection() {
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

        walletLedgerService.deposit(32, "USDT", new BigDecimal("1000"), "deposit-32");
        placeOrderUseCase.place(command(32, OrderSide.BUY, "100.00", "1.000"));
        cancelOnDisconnectService.register("ws-old-32", 32, "btcusdt");

        boolean resumed = cancelOnDisconnectService.resume("ws-old-32", "ws-new-32", 32);
        int oldCloseCanceled = cancelOnDisconnectService.cancelForConnection("ws-old-32");
        int newCloseCanceled = cancelOnDisconnectService.cancelForConnection("ws-new-32");

        assertThat(resumed).isTrue();
        assertThat(oldCloseCanceled).isZero();
        assertThat(newCloseCanceled).isEqualTo(1);
        assertThat(cancelOnDisconnectService.registeredCount()).isZero();
        assertThat(orderRepo.findAllOrders(32L, "BTCUSDT")).singleElement()
                .extracting(Order::getStatus)
                .isEqualTo(Order.Status.CANCELED);
        assertThat(accountRepo.findByUid(32).orElseThrow().crossOrderHold()).isEqualByComparingTo("0");

        matchingEngine.shutdown();
    }

    /**
     * 建立標準 BTCUSDT LIMIT 下單 command，讓各整合測試只改 uid、side、price、qty。
     */
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
                null,
                null,
                "GTC",
                false,
                false
        );
    }

    /**
     * 建立 BTCUSDT symbol config，重點可調 maxOpenOrders 以測 pre-trade risk 上限。
     */
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

    /**
     * 建立兩階 BTCUSDT risk tiers：tier2 要求 20% 初始保證金且最多 5 倍槓桿，總 notional 上限 2000。
     */
    private static SymbolConfig btcConfigWithRiskTiers() {
        return SymbolConfig.builder()
                .symbol("BTCUSDT")
                .baseAsset("BTC")
                .quoteAsset("USDT")
                .priceTick(new BigDecimal("0.01"))
                .lotSize(new BigDecimal("0.001"))
                .minQty(new BigDecimal("0.001"))
                .minNotional(new BigDecimal("5"))
                .maxOrderNotional(new BigDecimal("1000000"))
                .maxPositionNotional(new BigDecimal("2000"))
                .maxOpenOrders(200)
                .maxLeverage(20)
                .makerFeeRate(new BigDecimal("0.0002"))
                .takerFeeRate(new BigDecimal("0.0005"))
                .priceBandRate(new BigDecimal("0.10"))
                .initialMarginRate(new BigDecimal("0.05"))
                .maintenanceMarginRate(new BigDecimal("0.005"))
                .riskTiers(List.of(
                        SymbolConfig.RiskTier.builder()
                                .tier(1)
                                .maxPositionNotional(new BigDecimal("1000"))
                                .initialMarginRate(new BigDecimal("0.05"))
                                .maintenanceMarginRate(new BigDecimal("0.005"))
                                .maxLeverage(20)
                                .build(),
                        SymbolConfig.RiskTier.builder()
                                .tier(2)
                                .maxPositionNotional(new BigDecimal("2000"))
                                .initialMarginRate(new BigDecimal("0.20"))
                                .maintenanceMarginRate(new BigDecimal("0.010"))
                                .maxLeverage(5)
                                .build()
                ))
                .tradingEnabled(true)
                .build();
    }

    /**
     * 建立預設風控設定，測試可在個別案例中只開啟需要的 risk switch。
     */
    private static RiskControlsProperties riskControls() {
        return new RiskControlsProperties();
    }

    /**
     * 建立 pre-trade risk 測試用 limit order，支援切換 reduceOnly 來測全站 reduce-only mode。
     */
    private static Order limitOrder(long uid, Symbol symbol, OrderSide side, String price, boolean reduceOnly) {
        return limitOrder(uid, symbol, side, price, "1.000", 20, reduceOnly);
    }

    private static Order limitOrder(long uid, Symbol symbol, OrderSide side, String price, String qty, int leverage) {
        return limitOrder(uid, symbol, side, price, qty, leverage, false);
    }

    private static Order limitOrder(
            long uid,
            Symbol symbol,
            OrderSide side,
            String price,
            String qty,
            int leverage,
            boolean reduceOnly
    ) {
        return Order.builder()
                .uid(uid)
                .symbol(symbol)
                .side(side)
                .type(OrderType.LIMIT)
                .price(new BigDecimal(price))
                .qty(new BigDecimal(qty))
                .origQty(new BigDecimal(qty))
                .leverage(leverage)
                .reduceOnly(reduceOnly)
                .build();
    }

    /**
     * 共用風控開關拒單驗證鏈路：建立最小 RiskService -> preCheckAndReserve -> 檢查 rejectCode 與無 ledger。
     */
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
        /**
         * 依 uid 查帳戶；RiskService 與 WalletLedgerService 都透過它取得資金狀態。
         */
        public Optional<Account> findByUid(long uid) {
            return Optional.ofNullable(accounts.get(uid));
        }

        @Override
        /**
         * 寫回帳戶餘額、order hold 與 position margin，支援後續副作用驗證。
         */
        public void save(Account account) {
            accounts.put(account.uid(), account);
        }
    }

    private static class MemWalletLedgerRepository implements WalletLedgerRepository {
        private final List<WalletLedgerEntry> entries = new ArrayList<>();

        @Override
        /**
         * 追加帳務分錄，同時確認每筆 entry 都符合 balanced ledger baseline。
         */
        public void append(WalletLedgerEntry entry) {
            assertThat(entry.isBalanced()).isTrue();
            entries.add(entry);
        }

        @Override
        /**
         * 依 uid 讀取帳務分錄，用於驗證 reserve、fee、position margin release/increase。
         */
        public List<WalletLedgerEntry> findByUid(long uid) {
            return entries.stream().filter(entry -> entry.getUid() == uid).toList();
        }

        @Override
        /**
         * 依 refId 查帳務分錄，保留 WalletLedgerRepository contract 給 service 使用。
         */
        public List<WalletLedgerEntry> findByRefId(String refId) {
            return entries.stream().filter(entry -> refId.equals(entry.getRefId())).toList();
        }
    }

    private static class MemPositionRepository implements PositionRepository {
        private final Map<String, Position> positions = new LinkedHashMap<>();

        @Override
        /**
         * 依 uid + symbol 找持倉，OrderService 成交後會讀它來累加 position。
         */
        public Optional<Position> find(long uid, Symbol symbol) {
            return Optional.ofNullable(positions.get(key(uid, symbol.code())));
        }

        @Override
        /**
         * 保存成交或結算後的持倉狀態，供後續 assertion 檢查 qty、margin、fee。
         */
        public void save(Position position) {
            positions.put(key(position.getUid(), position.getSymbol().code()), position);
        }

        @Override
        /**
         * 回傳指定 uid 的全部持倉，支援 risk/reconciliation 類服務的 repository contract。
         */
        public List<Position> findAllByUid(long uid) {
            return positions.values().stream().filter(position -> position.getUid() == uid).toList();
        }

        @Override
        /**
         * 回傳所有 open positions；此整合測試主要保留 contract，讓 stub 接近真實 repository。
         */
        public List<Position> findOpenPositions() {
            return positions.values().stream()
                    .filter(position -> position.getQty() != null && position.getQty().signum() != 0)
                    .toList();
        }

        /**
         * 建立 uid + symbol 複合 key，避免同 uid 多市場或多 uid 同市場互相覆蓋。
         */
        private static String key(long uid, String symbol) {
            return uid + ":" + symbol;
        }
    }

    private static MatchingWorkerLifecycleService readyWorkerLifecycle(String symbolCode, MatchingSequencerLease lease) {
        return new MatchingWorkerLifecycleService(null, null, null, null, null) {
            @Override
            public Optional<MatchingWorkerOwnerContext> ownerContext(String requestedSymbol) {
                if (!symbolCode.equalsIgnoreCase(requestedSymbol)) {
                    return Optional.empty();
                }
                return Optional.of(new MatchingWorkerOwnerContext(
                        symbolCode,
                        lease.ownerId(),
                        lease.epoch(),
                        lease.expiresAt(),
                        lease.commandOffset(),
                        lease.eventOffset(),
                        lease.updatedAt()
                ));
            }
        };
    }

    private static final class TestLeaseStore implements MatchingSequencerLeaseStore {
        private final Map<String, MatchingSequencerLease> leases = new ConcurrentHashMap<>();

        @Override
        /**
         * 測試用 acquire：同 owner 沿用 epoch，其他 owner 只能在過期後 takeover。
         */
        public Optional<MatchingSequencerLease> acquire(String symbolCode, String ownerId, Duration ttl, Instant now) {
            String symbol = normalize(symbolCode);
            MatchingSequencerLease current = leases.get(symbol);
            if (current != null && !current.ownerId().equals(ownerId) && current.expiresAt().isAfter(now)) {
                return Optional.empty();
            }
            long nextEpoch = current == null
                    ? 1L
                    : current.ownerId().equals(ownerId) ? current.epoch() : current.epoch() + 1;
            MatchingSequencerLease acquired = new MatchingSequencerLease(
                    symbol,
                    ownerId,
                    nextEpoch,
                    now.plus(ttl),
                    current == null ? 0L : current.commandOffset(),
                    current == null ? 0L : current.eventOffset(),
                    now
            );
            leases.put(symbol, acquired);
            return Optional.of(acquired);
        }

        @Override
        /**
         * 續租不是此測試重點；保留實作讓 store 符合 contract。
         */
        public Optional<MatchingSequencerLease> renew(
                String symbolCode,
                String ownerId,
                long epoch,
                Duration ttl,
                long commandOffset,
                long eventOffset,
                Instant now
        ) {
            String symbol = normalize(symbolCode);
            MatchingSequencerLease current = leases.get(symbol);
            if (current == null || !current.ownerId().equals(ownerId) || current.epoch() != epoch) {
                return Optional.empty();
            }
            MatchingSequencerLease renewed = new MatchingSequencerLease(
                    symbol,
                    ownerId,
                    epoch,
                    now.plus(ttl),
                    commandOffset,
                    eventOffset,
                    now
            );
            leases.put(symbol, renewed);
            return Optional.of(renewed);
        }

        @Override
        /**
         * release 成功時移除 lease，用於維持測試 store 的完整 contract。
         */
        public boolean release(String symbolCode, String ownerId, long epoch, Instant now) {
            String symbol = normalize(symbolCode);
            MatchingSequencerLease current = leases.get(symbol);
            if (current == null || !current.ownerId().equals(ownerId) || current.epoch() != epoch) {
                return false;
            }
            leases.remove(symbol);
            return true;
        }

        @Override
        /**
         * 回傳目前 symbol lease，供 `requireWritable` 驗證 owner/epoch。
         */
        public Optional<MatchingSequencerLease> current(String symbolCode) {
            return Optional.ofNullable(leases.get(normalize(symbolCode)));
        }
    }

    private static String normalize(String symbolCode) {
        return symbolCode == null ? "" : symbolCode.trim().toUpperCase();
    }

    private static final class MutableClock extends Clock {
        private final Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }

    private static class MemOrderRepository implements OrderRepository {
        private final Map<UUID, Order> orders = new LinkedHashMap<>();

        @Override
        /**
         * 依 order id 查訂單，改單、撤單、cancel-replace 都從這裡定位原單。
         */
        public Optional<Order> findById(UUID id) {
            return Optional.ofNullable(orders.get(id));
        }

        @Override
        /**
         * 保存訂單狀態，包含 NEW、FILLED、CANCELED、REJECTED 等 lifecycle 變化。
         */
        public void save(Order order) {
            orders.put(order.getId(), order);
        }

        @Override
        /**
         * 查 uid 下全部 open orders，pre-trade risk 用它計算 maxOpenOrders 與 clientOrderId dedupe。
         */
        public List<Order> openOrders(long uid) {
            return orders.values().stream()
                    .filter(order -> order.getUid() == uid)
                    .filter(order -> order.getStatus() == Order.Status.NEW
                            || order.getStatus() == Order.Status.PARTIALLY_FILLED)
                    .toList();
        }

        @Override
        /**
         * 查指定 uid/symbol 的 open orders，bulk cancel 與 cancel-on-disconnect 會走這個入口。
         */
        public List<Order> findOpenOrders(Long uid, String symbol) {
            return openOrders(uid).stream()
                    .filter(order -> order.getSymbol().code().equalsIgnoreCase(symbol))
                    .toList();
        }

        @Override
        /**
         * 查指定 uid/symbol 的全部訂單，用於測試確認原單與 replacement order 的最終狀態。
         */
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
        /**
         * 模擬 trade event append，回傳遞增 sequence 讓 OrderService 可完成事件落點。
         */
        public long append(TradeExecuted event) {
            return seq.incrementAndGet();
        }

        @Override
        /**
         * 回傳目前最後 sequence，維持 EventStore contract 以支援 market/order event chain。
         */
        public long lastSeq(long uid) {
            return seq.get();
        }
    }

    private static class MemIdempotencyRepository implements IdempotencyRepository {
        private final HashSet<String> keys = new HashSet<>();

        @Override
        /**
         * 插入 idempotency key；首次成功、重複失敗，模擬 production 去重語意。
         */
        public boolean insertIfAbsent(String key, Instant expiresAt) {
            return keys.add(key);
        }

        @Override
        /**
         * 查 key 是否存在，支援 service 在同一測試流程中確認重複請求。
         */
        public boolean exists(String key) {
            return keys.contains(key);
        }
    }
}
