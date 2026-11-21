package com.lumix.trading.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumix.account.AccountId;
import com.lumix.account.AssetSymbol;
import com.lumix.admin.superadmin.SuperAdminAccessService;
import com.lumix.common.RequestId;
import com.lumix.ledger.application.posting.LedgerPostingCommand;
import com.lumix.ledger.domain.*;
import com.lumix.ledger.runtime.*;
import com.lumix.user.auth.domain.AuthenticatedUser;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** 隔離 PostgreSQL 驗證政策發佈的版本、帳戶責任與 HALTED 閉鎖。 */
class FuturesRiskPolicyAdministrationIntegrationTest {
    @Test
    void policyIsImmutableIdempotentAndDoesNotOpenTradingOrCreateFunds() throws Exception {
        try (EmbeddedPostgres postgres = EmbeddedPostgres.start()) {
            var dataSource = postgres.getPostgresDatabase();
            Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load().migrate();
            var jdbc = new JdbcTemplate(dataSource);
            var transaction = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
            jdbc.update("INSERT INTO assets(asset_symbol,display_name,precision_scale,status) VALUES "
                    + "('BTC','Bitcoin',8,'ACTIVE'),('USDT','Tether',6,'ACTIVE') ON CONFLICT DO NOTHING");
            jdbc.update("INSERT INTO markets(market_symbol,base_asset_symbol,quote_asset_symbol,price_scale,quantity_scale,status) "
                    + "VALUES ('BTC-USDT','BTC','USDT',2,4,'ACTIVE') ON CONFLICT DO NOTHING");
            account(jdbc, "futures-fees", "TRADING_FEE_REVENUE");
            account(jdbc, "futures-margin", "PERPETUAL_MARGIN_POOL");
            account(jdbc, "futures-insurance", "PERPETUAL_INSURANCE");
            account(jdbc, "futures-pool", "PERPETUAL_CONVERSION_POOL");
            var ledger = new TransactionalLedgerPostingService(jdbc, new LedgerInvariantPolicy(),
                    new LedgerBalanceProjectionUpdater(jdbc));
            var support = new TradingExecutionSupport(jdbc, new ObjectMapper().findAndRegisterModules(), ledger);
            var service = new FuturesRiskPolicyAdministrationService(jdbc, support,
                    mock(SuperAdminAccessService.class));
            var actor = new AuthenticatedUser("admin", "admin@example.test", "Admin");
            var first = policy(1, "futures-insurance");
            transaction.execute(status -> service.publish(actor, first));
            transaction.execute(status -> service.publish(actor, first));
            assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM futures_risk_policies", Integer.class));
            assertEquals("HALTED", jdbc.queryForObject("SELECT status FROM trading_instruments WHERE instrument_id='PERPETUAL:BTCUSDT'", String.class));
            assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM ledger_journals", Integer.class));
            assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM audit_logs WHERE action_type='FUTURES_RISK_POLICY_PUBLISHED'", Integer.class));
            assertThrows(IllegalArgumentException.class, () -> transaction.execute(status ->
                    service.publish(actor, policy(1, "futures-insurance", "900000"))));
            assertThrows(IllegalArgumentException.class, () -> transaction.execute(status ->
                    service.publish(actor, policy(3, "futures-insurance"))));
            assertThrows(IllegalArgumentException.class, () -> transaction.execute(status ->
                    service.publish(actor, policy(2, "missing-insurance"))));
            assertThrows(org.springframework.dao.DataAccessException.class, () -> jdbc.update(
                    "UPDATE futures_risk_policies SET created_by='other' WHERE instrument_id='PERPETUAL:BTCUSDT'"));

            userAccount(jdbc, "alice");
            userAccount(jdbc, "bob");
            account(jdbc, "treasury", null);
            transaction.executeWithoutResult(status -> {
                seed(ledger, "alice");
                seed(ledger, "bob");
            });
            var positions = new FuturesHedgePositionStore(jdbc, new ObjectMapper().findAndRegisterModules());
            var opened = transaction.execute(status -> {
                fill(jdbc, ledger, "open", "bob", "SELL", "SHORT", false,
                        "alice", "BUY", "LONG", false, "100", "0.1");
                return List.of(positions.recordFill("trade_open", "alice_open", new BigDecimal("2")),
                        positions.recordFill("trade_open", "bob_open", new BigDecimal("2")));
            });
            var openedLong = opened.get(0);
            var openedShort = opened.get(1);
            assertEquals(new BigDecimal("0.1"), openedLong.quantity().stripTrailingZeros());
            assertEquals(new BigDecimal("0.1"), openedShort.quantity().stripTrailingZeros());
            assertEquals(0, new BigDecimal("2").compareTo(transaction.execute(status ->
                    positions.recordFill("trade_open", "alice_open", new BigDecimal("999"))).isolatedCollateral()));
            var partial = transaction.execute(status -> {
                fill(jdbc, ledger, "close1", "bob", "BUY", "SHORT", true,
                        "alice", "SELL", "LONG", true, "110", "0.05");
                assertThrows(IllegalArgumentException.class, () -> positions.recordFill(
                        "trade_close1", "alice_close1", new BigDecimal("0.5")));
                return List.of(positions.recordFill("trade_close1", "alice_close1", BigDecimal.ONE),
                        positions.recordFill("trade_close1", "bob_close1", BigDecimal.ONE));
            });
            assertEquals(0, new BigDecimal("0.05").compareTo(partial.get(0).quantity()));
            var closed = transaction.execute(status -> {
                fill(jdbc, ledger, "close2", "bob", "BUY", "SHORT", true,
                        "alice", "SELL", "LONG", true, "110", "0.05");
                return List.of(positions.recordFill("trade_close2", "alice_close2", BigDecimal.ZERO),
                        positions.recordFill("trade_close2", "bob_close2", BigDecimal.ZERO));
            });
            var closedLong = closed.get(0);
            var closedShort = closed.get(1);
            assertEquals(new BigDecimal("0.500000"), closedLong.realizedPnlUsdt());
            assertEquals(new BigDecimal("-0.500000"), closedShort.realizedPnlUsdt());
            assertEquals(6, jdbc.queryForObject("SELECT count(*) FROM futures_hedge_position_events", Integer.class));
            assertEquals(0, new BigDecimal("11").compareTo(jdbc.queryForObject(
                    "SELECT total_amount FROM balance_projections WHERE account_id='alice' AND asset_symbol='USDT'",
                    BigDecimal.class)));
            assertEquals(0, new BigDecimal("9").compareTo(jdbc.queryForObject(
                    "SELECT total_amount FROM balance_projections WHERE account_id='bob' AND asset_symbol='USDT'",
                    BigDecimal.class)));
            assertThrows(IllegalArgumentException.class, () -> transaction.execute(status ->
                    positions.recordFill("trade_open", "alice_close1", new BigDecimal("2"))));
        }
    }

    private static void account(JdbcTemplate jdbc, String id, String purpose) {
        jdbc.update("INSERT INTO users(user_id,email,display_name,status) VALUES (?,?,?,'SUSPENDED')",
                id, id + "@example.test", id);
        jdbc.update("INSERT INTO accounts(account_id,user_id,account_type,status,account_category,account_purpose) "
                + "VALUES (?,?,'FUTURES','ACTIVE','EXCHANGE',?)", id, id, purpose);
        jdbc.update("INSERT INTO account_assets(account_id,asset_symbol,status) VALUES (?,'USDT','ACTIVE')", id);
    }

    private static void userAccount(JdbcTemplate jdbc, String user) {
        jdbc.update("INSERT INTO users(user_id,email,display_name,status) VALUES (?,?,?,'ACTIVE')",
                user, user + "@example.test", user);
        jdbc.update("INSERT INTO accounts(account_id,user_id,account_type,status,account_category) "
                + "VALUES (?,?,'FUTURES','ACTIVE','USER')", user, user);
        jdbc.update("INSERT INTO account_assets(account_id,asset_symbol,status) VALUES (?,'USDT','ACTIVE')", user);
    }

    /** 這些資料只存在隔離測試資料庫，用來驗證持倉記錄對既有成交的嚴格關聯。 */
    private static void fill(JdbcTemplate jdbc, TransactionalLedgerPostingService ledger,
                             String suffix, String maker, String makerSide,
                             String makerPosition, boolean makerReduce, String taker, String takerSide,
                             String takerPosition, boolean takerReduce, String price, String quantity) {
        for (String owner : new String[] { maker, taker }) {
            String id = owner + "_" + suffix;
            String side = owner.equals(maker) ? makerSide : takerSide;
            String position = owner.equals(maker) ? makerPosition : takerPosition;
            boolean reduce = owner.equals(maker) ? makerReduce : takerReduce;
            jdbc.update("INSERT INTO orders(order_id,user_id,account_id,market_symbol,side,order_type,"
                            + "time_in_force,status,price,quantity,filled_quantity,remaining_quantity,request_id) "
                            + "VALUES (?,?,?,'BTC-USDT',?,'LIMIT','GTC','FILLED',?,?,?,0,?)",
                    id, owner, owner, side, new BigDecimal(price), new BigDecimal(quantity),
                    new BigDecimal(quantity), "request_" + id);
            jdbc.update("INSERT INTO trading_order_executions(order_id,instrument_id,policy_version,"
                            + "request_hash,position_side,reduce_only,leverage) "
                            + "VALUES (?,'PERPETUAL:BTCUSDT',1,?,?,?,5)",
                    id, "hash_" + id, position, reduce);
        }
        String trade = "trade_" + suffix;
        List<LedgerEntryDraft> entries = "open".equals(suffix)
                ? List.of(entry("bob", LedgerDirection.DEBIT, "2", 1),
                entry("alice", LedgerDirection.DEBIT, "2", 2),
                entry("futures-margin", LedgerDirection.CREDIT, "4", 3))
                : List.of(entry("futures-margin", LedgerDirection.DEBIT, "2", 1),
                entry("alice", LedgerDirection.CREDIT, "1.5", 2),
                entry("bob", LedgerDirection.CREDIT, "0.5", 3));
        long journal = ledger.post(new LedgerPostingExecutionCommand(
                new LedgerPostingCommand(new RequestId("request_" + trade),
                        new LedgerJournalDraft(LedgerBusinessReferenceType.TRADE, trade, entries), Instant.now()),
                "ledger_" + trade, new LedgerPostingActor("SYSTEM", "futures-test"))).ledgerJournalId();
        jdbc.update("INSERT INTO trades(trade_id,market_symbol,maker_order_id,taker_order_id,"
                        + "maker_account_id,taker_account_id,price,quantity,quote_quantity,"
                        + "maker_fee_amount,taker_fee_amount,fee_asset_symbol) "
                        + "VALUES (?,'BTC-USDT',?,?,?,?,?,?,?,0,0,'USDT')",
                trade, maker + "_" + suffix, taker + "_" + suffix, maker, taker,
                new BigDecimal(price), new BigDecimal(quantity),
                new BigDecimal(price).multiply(new BigDecimal(quantity)));
        jdbc.update("INSERT INTO trading_trade_executions(trade_id,instrument_id,maker_policy_version,"
                        + "taker_policy_version,ledger_journal_id) VALUES (?,'PERPETUAL:BTCUSDT',1,1,?)",
                trade, journal);
    }

    private static void seed(TransactionalLedgerPostingService ledger, String user) {
        String reference = "seed_" + user;
        ledger.post(new LedgerPostingExecutionCommand(
                new LedgerPostingCommand(new RequestId(reference),
                        new LedgerJournalDraft(LedgerBusinessReferenceType.ADJUSTMENT, reference,
                                List.of(entry("treasury", LedgerDirection.DEBIT, "10", 1),
                                        entry(user, LedgerDirection.CREDIT, "10", 2))), Instant.now()),
                reference, new LedgerPostingActor("SYSTEM", "futures-test")));
    }

    private static LedgerEntryDraft entry(String account, LedgerDirection direction, String amount, int sequence) {
        return new LedgerEntryDraft(new AccountId(account), new AssetSymbol("USDT"),
                direction, new BigDecimal(amount), sequence);
    }

    private static FuturesRiskPolicy policy(long version, String insurance) {
        return policy(version, insurance, "1000000");
    }

    private static FuturesRiskPolicy policy(long version, String insurance, String positionLimit) {
        var rules = new TradingInstrumentRules("PERPETUAL:BTCUSDT", TradingInstrumentRules.Kind.PERPETUAL,
                "BTC-USDT", "BTC", "USDT", version, 2, 4, 6, new BigDecimal("0.01"),
                new BigDecimal("0.0001"), new BigDecimal("0.0001"), new BigDecimal("100"),
                BigDecimal.ONE, new BigDecimal("10000000"), new BigDecimal("0.001"),
                new BigDecimal("0.002"), RoundingMode.DOWN);
        var execution = new TradingExecutionPolicy(rules, "futures-fees", 100,
                new BigDecimal("0.05"), TradingExecutionPolicy.FeeAccrual.CUMULATIVE_QUOTE,
                TradingExecutionPolicy.CounterpartyModel.CLIENT_TO_CLIENT);
        return new FuturesRiskPolicy(execution, "USDT", BigDecimal.ZERO, new BigDecimal("5"),
                new BigDecimal("0.05"), new BigDecimal(positionLimit),
                "BINANCE_USDSM_PREMIUM_INDEX", "BINANCE_USDSM_PREMIUM_INDEX", Duration.ofSeconds(10),
                Instant.EPOCH, Duration.ofHours(8), new BigDecimal("0.01"), new BigDecimal("100000"),
                RoundingMode.DOWN,
                "futures-margin", insurance, "futures-pool");
    }
}
