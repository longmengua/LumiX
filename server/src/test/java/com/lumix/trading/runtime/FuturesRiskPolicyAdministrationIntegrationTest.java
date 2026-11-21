package com.lumix.trading.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumix.admin.superadmin.SuperAdminAccessService;
import com.lumix.ledger.runtime.TransactionalLedgerPostingService;
import com.lumix.user.auth.domain.AuthenticatedUser;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
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
            account(jdbc, "futures-insurance", "PERPETUAL_INSURANCE");
            account(jdbc, "futures-pool", "PERPETUAL_CONVERSION_POOL");
            var support = new TradingExecutionSupport(jdbc, new ObjectMapper().findAndRegisterModules(),
                    mock(TransactionalLedgerPostingService.class));
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
        }
    }

    private static void account(JdbcTemplate jdbc, String id, String purpose) {
        jdbc.update("INSERT INTO users(user_id,email,display_name,status) VALUES (?,?,?,'SUSPENDED')",
                id, id + "@example.test", id);
        jdbc.update("INSERT INTO accounts(account_id,user_id,account_type,status,account_category,account_purpose) "
                + "VALUES (?,?,'FUTURES','ACTIVE','EXCHANGE',?)", id, id, purpose);
        jdbc.update("INSERT INTO account_assets(account_id,asset_symbol,status) VALUES (?,'USDT','ACTIVE')", id);
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
                Duration.ofHours(8), new BigDecimal("0.01"), new BigDecimal("100000"),
                insurance, "futures-pool");
    }
}
