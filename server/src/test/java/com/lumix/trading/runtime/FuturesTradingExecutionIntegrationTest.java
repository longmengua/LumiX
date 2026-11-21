package com.lumix.trading.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumix.account.AccountId;
import com.lumix.account.AssetSymbol;
import com.lumix.admin.superadmin.SuperAdminAccessService;
import com.lumix.common.RequestId;
import com.lumix.ledger.application.posting.LedgerPostingCommand;
import com.lumix.ledger.domain.*;
import com.lumix.ledger.runtime.*;
import com.lumix.marketfeed.binance.BinanceFuturesReferencePriceFeed;
import com.lumix.reservation.OrderReservationChangeService;
import com.lumix.reservation.ReservationHoldService;
import com.lumix.user.auth.domain.AuthenticatedUser;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** 隔離 PostgreSQL 執行兩帳戶 USDT 開倉、撮合、平倉與帳本守恆。 */
class FuturesTradingExecutionIntegrationTest {
    private static final String SOURCE = "BINANCE_USDSM_PREMIUM_INDEX";

    @Test
    void oppositeHedgeOrdersOpenAndCloseWithRealLedgerBalances() throws Exception {
        try (EmbeddedPostgres postgres = EmbeddedPostgres.start()) {
            var dataSource = postgres.getPostgresDatabase();
            Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load().migrate();
            var jdbc = new JdbcTemplate(dataSource);
            var transaction = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
            var json = new ObjectMapper().findAndRegisterModules();
            jdbc.update("INSERT INTO assets(asset_symbol,display_name,precision_scale,status) "
                    + "VALUES ('BTC','Bitcoin',8,'ACTIVE'),('USDT','Tether',6,'ACTIVE')");
            jdbc.update("INSERT INTO markets(market_symbol,base_asset_symbol,quote_asset_symbol,price_scale,"
                    + "quantity_scale,status) VALUES ('BTC-USDT','BTC','USDT',2,4,'ACTIVE')");
            account(jdbc, "alice", true, null);
            account(jdbc, "bob", true, null);
            account(jdbc, "fees", false, "TRADING_FEE_REVENUE");
            account(jdbc, "margin", false, "PERPETUAL_MARGIN_POOL");
            account(jdbc, "insurance", false, "PERPETUAL_INSURANCE");
            account(jdbc, "conversion", false, "PERPETUAL_CONVERSION_POOL");
            account(jdbc, "treasury", false, null);
            var ledger = new TransactionalLedgerPostingService(jdbc, new LedgerInvariantPolicy(),
                    new LedgerBalanceProjectionUpdater(jdbc));
            var support = new TradingExecutionSupport(jdbc, json, ledger);
            var admin = new FuturesRiskPolicyAdministrationService(jdbc, support,
                    mock(SuperAdminAccessService.class));
            var riskPolicy = policy();
            transaction.execute(status -> admin.publish(
                    new AuthenticatedUser("alice", "alice@example.test", "Alice"), riskPolicy));
            transaction.executeWithoutResult(status -> {
                seed(ledger, "alice");
                seed(ledger, "bob");
                seed(ledger, "insurance");
            });
            var feed = mock(BinanceFuturesReferencePriceFeed.class);
            when(feed.current(anyString(), any(), any())).thenAnswer(invocation -> {
                Instant observed = Instant.now().minusMillis(100);
                return Optional.of(new BinanceFuturesReferencePriceFeed.Snapshot(SOURCE, "BTCUSDT",
                        new BigDecimal("100"), new BigDecimal("100"), observed, observed));
            });
            @SuppressWarnings("unchecked")
            ObjectProvider<BinanceFuturesReferencePriceFeed> provider = mock(ObjectProvider.class);
            when(provider.getIfAvailable()).thenReturn(feed);
            var engine = new FuturesTradingExecutionService(jdbc,
                    new FuturesRiskPolicyRepository(jdbc, json), support,
                    new ReservationHoldService(jdbc), new OrderReservationChangeService(jdbc, json),
                    new TradingOrderQueryService(jdbc), provider, json);
            var statusService = new FuturesInstrumentStatusService(jdbc,
                    new FuturesRiskPolicyRepository(jdbc, json), support, provider,
                    mock(SuperAdminAccessService.class), engine);
            assertEquals("ACTIVE", transaction.execute(status -> statusService.change(
                    new AuthenticatedUser("alice", "alice@example.test", "Alice"),
                    "BTC-USDT", "ACTIVE")).status());

            var shortOrder = place(transaction, engine, "bob", "SELL", "SHORT", false, "100", "short-open");
            assertEquals("OPEN", shortOrder.status());
            var longOrder = place(transaction, engine, "alice", "BUY", "LONG", false, "100", "long-open");
            assertEquals("FILLED", longOrder.status());
            assertEquals("FILLED", new TradingOrderQueryService(jdbc)
                    .detail("bob", "futures", shortOrder.orderId()).status());
            balance(jdbc, "alice", "8");
            balance(jdbc, "bob", "8");
            balance(jdbc, "margin", "4");
            assertEquals(2, count(jdbc, "futures_hedge_position_events"));
            assertEquals(longOrder.orderId(), place(transaction, engine, "alice", "BUY", "LONG",
                    false, "100", "long-open").orderId());

            var funding = new FuturesFundingSettlementService(jdbc,
                    new FuturesRiskPolicyRepository(jdbc, json), support, ledger, provider,
                    mock(SuperAdminAccessService.class));
            Instant periodEnd = riskPolicy.fundingScheduleAnchor().plus(riskPolicy.fundingInterval());
            var cycle = transaction.execute(status -> funding.settle(
                    new AuthenticatedUser("alice", "alice@example.test", "Alice"),
                    "BTC-USDT", periodEnd, new BigDecimal("0.01")));
            assertEquals(2, cycle.positionCount());
            assertEquals(cycle.cycleId(), transaction.execute(status -> funding.settle(
                    new AuthenticatedUser("alice", "alice@example.test", "Alice"),
                    "BTC-USDT", periodEnd, new BigDecimal("0.01"))).cycleId());
            balance(jdbc, "margin", "3.9");
            balance(jdbc, "bob", "8.1");
            assertEquals(1, count(jdbc, "futures_funding_cycles"));
            assertThrows(IllegalArgumentException.class, () -> transaction.execute(status -> funding.settle(
                    new AuthenticatedUser("alice", "alice@example.test", "Alice"),
                    "BTC-USDT", periodEnd, new BigDecimal("-0.01"))));

            var longClose = place(transaction, engine, "alice", "SELL", "LONG", true, "110", "long-close");
            assertEquals("OPEN", longClose.status());
            var shortClose = place(transaction, engine, "bob", "BUY", "SHORT", true, "110", "short-close");
            assertEquals("FILLED", shortClose.status());
            balance(jdbc, "alice", "10.9");
            balance(jdbc, "bob", "9.1");
            balance(jdbc, "margin", "0");
            assertEquals(4, count(jdbc, "futures_hedge_position_events"));
            assertEquals(2, count(jdbc, "trades"));
            assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM futures_hedge_positions WHERE quantity>0",
                    Integer.class));
            assertEquals(0, jdbc.queryForObject("SELECT COALESCE(SUM(CASE WHEN direction='CREDIT' "
                    + "THEN amount ELSE -amount END),0) FROM ledger_entries WHERE asset_symbol='USDT'",
                    BigDecimal.class).signum());
            assertThrows(TradingOrderRejectedException.class, () -> place(transaction, engine, "alice",
                    "SELL", "LONG", true, "110", "over-close"));
            var resting = place(transaction, engine, "alice", "BUY", "LONG", false, "90", "resting");
            assertEquals("OPEN", resting.status());
            assertEquals("CANCELED", transaction.execute(status ->
                    engine.cancel("alice", resting.orderId(), "cancel-resting")).status());
            assertEquals("CANCELED", transaction.execute(status ->
                    engine.cancel("alice", resting.orderId(), "cancel-resting")).status());
            assertEquals(0, jdbc.queryForObject("SELECT locked_amount FROM balance_projections "
                    + "WHERE account_id='alice' AND asset_symbol='USDT'", BigDecimal.class).signum());
            assertThrows(java.util.NoSuchElementException.class, () -> transaction.execute(status ->
                    engine.cancel("bob", resting.orderId(), "steal-resting")));
            assertThrows(TradingOrderRejectedException.class, () -> transaction.execute(status ->
                    engine.place("alice", new FuturesTradingExecutionService.Command("BTC-USDT", "BUY",
                            "LONG", false, "LIMIT", "FOK", "90", "0.1", "5"), "fok-empty")));
            var selfMaker = place(transaction, engine, "alice", "SELL", "SHORT", false, "100", "self-maker");
            assertThrows(TradingOrderRejectedException.class, () -> place(transaction, engine, "alice",
                    "BUY", "LONG", false, "100", "self-taker"));
            transaction.execute(status -> engine.cancel("alice", selfMaker.orderId(), "cancel-self"));
            when(feed.current(anyString(), any(), any())).thenReturn(Optional.empty());
            assertThrows(TradingOrderRejectedException.class, () -> place(transaction, engine, "alice",
                    "BUY", "LONG", false, "100", "stale-mark"));

            when(feed.current(anyString(), any(), any())).thenAnswer(invocation -> {
                Instant observed = Instant.now().minusMillis(100);
                return Optional.of(new BinanceFuturesReferencePriceFeed.Snapshot(SOURCE, "BTCUSDT",
                        new BigDecimal("100"), new BigDecimal("100"), observed, observed));
            });
            place(transaction, engine, "bob", "SELL", "SHORT", false, "100", "short-reopen");
            place(transaction, engine, "alice", "BUY", "LONG", false, "100", "long-reopen");
            when(feed.current(anyString(), any(), any())).thenAnswer(invocation -> {
                Instant observed = Instant.now().minusMillis(100);
                return Optional.of(new BinanceFuturesReferencePriceFeed.Snapshot(SOURCE, "BTCUSDT",
                        BigDecimal.TEN, BigDecimal.TEN, observed, observed));
            });
            assertThrows(TradingOrderRejectedException.class, () -> transaction.execute(status ->
                    engine.liquidate("bob", "BTC-USDT", "SHORT", "healthy-short")));
            place(transaction, engine, "bob", "BUY", "LONG", false, "20", "liquidation-maker");
            var liquidation = transaction.execute(status -> engine.liquidate(
                    "alice", "BTC-USDT", "LONG", "liquidation-alice"));
            assertEquals("FILLED", liquidation.status());
            assertEquals(liquidation.orderId(), transaction.execute(status -> engine.liquidate(
                    "alice", "BTC-USDT", "LONG", "liquidation-alice")).orderId());
            assertEquals(1, count(jdbc, "futures_liquidation_attempts"));
            assertEquals("LIQUIDATE", jdbc.queryForObject("SELECT event_type FROM futures_hedge_position_events "
                    + "WHERE account_id='alice' ORDER BY next_version DESC LIMIT 1", String.class));
            balance(jdbc, "insurance", "4");
            assertEquals(0, jdbc.queryForObject("SELECT COALESCE(SUM(CASE WHEN direction='CREDIT' "
                    + "THEN amount ELSE -amount END),0) FROM ledger_entries WHERE asset_symbol='USDT'",
                    BigDecimal.class).signum());
            var pendingRisk = place(transaction, engine, "alice", "BUY", "LONG", false, "30", "pending-risk");
            assertEquals("OPEN", pendingRisk.status());
            transaction.executeWithoutResult(status -> statusService.riskHalt("BTC-USDT", "NO_LIQUIDITY"));
            assertEquals("REDUCE_ONLY", jdbc.queryForObject("SELECT status FROM trading_instruments "
                    + "WHERE instrument_id='PERPETUAL:BTCUSDT'", String.class));
            assertEquals("CANCELED", new TradingOrderQueryService(jdbc)
                    .detail("alice", "futures", pendingRisk.orderId()).status());
            assertThrows(TradingOrderRejectedException.class, () -> place(transaction, engine, "alice",
                    "BUY", "LONG", false, "30", "halted-open"));
        }
    }

    private static TradingOrderQueryService.OrderView place(TransactionTemplate transaction,
            FuturesTradingExecutionService engine, String owner, String side, String position,
            boolean reduce, String price, String key) {
        return transaction.execute(status -> engine.place(owner,
                new FuturesTradingExecutionService.Command("BTC-USDT", side, position, reduce,
                        "LIMIT", "GTC", price, "0.1", "5"), key));
    }

    private static void account(JdbcTemplate jdbc, String id, boolean user, String purpose) {
        jdbc.update("INSERT INTO users(user_id,email,display_name,status) VALUES (?,?,?,?)",
                id, id + "@example.test", id, user ? "ACTIVE" : "SUSPENDED");
        jdbc.update("INSERT INTO accounts(account_id,user_id,account_type,status,account_category,account_purpose) "
                        + "VALUES (?,?,'FUTURES','ACTIVE',?,?)",
                id, id, user ? "USER" : "EXCHANGE", purpose);
        jdbc.update("INSERT INTO account_assets(account_id,asset_symbol,status) VALUES (?,'USDT','ACTIVE')", id);
    }

    private static void seed(TransactionalLedgerPostingService ledger, String user) {
        String id = "seed_" + user;
        ledger.post(new LedgerPostingExecutionCommand(new LedgerPostingCommand(new RequestId(id),
                new LedgerJournalDraft(LedgerBusinessReferenceType.ADJUSTMENT, id, List.of(
                        new LedgerEntryDraft(new AccountId("treasury"), new AssetSymbol("USDT"),
                                LedgerDirection.DEBIT, new BigDecimal("10"), 1),
                        new LedgerEntryDraft(new AccountId(user), new AssetSymbol("USDT"),
                                LedgerDirection.CREDIT, new BigDecimal("10"), 2))), Instant.now()),
                id, new LedgerPostingActor("SYSTEM", "futures-test")));
    }

    private static FuturesRiskPolicy policy() {
        var rules = new TradingInstrumentRules("PERPETUAL:BTCUSDT", TradingInstrumentRules.Kind.PERPETUAL,
                "BTC-USDT", "BTC", "USDT", 1, 2, 4, 6, new BigDecimal("0.01"),
                new BigDecimal("0.0001"), new BigDecimal("0.0001"), new BigDecimal("100"),
                BigDecimal.ONE, new BigDecimal("10000000"), BigDecimal.ZERO, BigDecimal.ZERO,
                RoundingMode.DOWN);
        var execution = new TradingExecutionPolicy(rules, "fees", 100, new BigDecimal("0.05"),
                TradingExecutionPolicy.FeeAccrual.CUMULATIVE_QUOTE,
                TradingExecutionPolicy.CounterpartyModel.CLIENT_TO_CLIENT);
        return new FuturesRiskPolicy(execution, "USDT", BigDecimal.ZERO, new BigDecimal("5"),
                new BigDecimal("0.05"), new BigDecimal("1000000"), SOURCE, SOURCE,
                Duration.ofSeconds(30), Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS)
                        .minus(Duration.ofHours(8)), Duration.ofHours(8), new BigDecimal("0.01"),
                new BigDecimal("100000"), RoundingMode.DOWN, "margin", "insurance", "conversion");
    }

    private static void balance(JdbcTemplate jdbc, String account, String expected) {
        BigDecimal amount = jdbc.queryForObject("SELECT account_category FROM accounts WHERE account_id=?",
                String.class, account).equals("EXCHANGE")
                ? jdbc.queryForObject("SELECT COALESCE(SUM(CASE WHEN direction='CREDIT' THEN amount "
                        + "ELSE -amount END),0) FROM ledger_entries WHERE account_id=? AND asset_symbol='USDT'",
                        BigDecimal.class, account)
                : jdbc.queryForObject("SELECT total_amount FROM balance_projections "
                        + "WHERE account_id=? AND asset_symbol='USDT'", BigDecimal.class, account);
        assertEquals(0, new BigDecimal(expected).compareTo(amount));
    }

    private static int count(JdbcTemplate jdbc, String table) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
    }
}
