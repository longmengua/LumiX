package com.lumix.trading.liquidity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumix.admin.superadmin.SuperAdminAccessService;
import com.lumix.user.auth.domain.AuthenticatedUser;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.util.Map;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** 隔離 PostgreSQL 驗證預覽費率的版本、不可變稽核及不啟用交易的邊界。 */
class ExternalQuotePreviewPolicyServiceIntegrationTest {
    @Test
    void appendOnlyPoliciesAreIdempotentAndDoNotCreateTradesOrBalances() throws Exception {
        try (EmbeddedPostgres postgres = EmbeddedPostgres.start()) {
            var source = postgres.getPostgresDatabase();
            Flyway.configure().dataSource(source).locations("classpath:db/migration").load().migrate();
            var jdbc = new JdbcTemplate(source);
            jdbc.update("INSERT INTO users(user_id,email,display_name,status) "
                    + "VALUES ('admin','admin@example.test','Admin','ACTIVE')");
            var service = new ExternalQuotePreviewPolicyService(jdbc, new ObjectMapper(),
                    mock(SuperAdminAccessService.class));
            var transaction = new TransactionTemplate(new DataSourceTransactionManager(source));
            var actor = new AuthenticatedUser("admin", "admin@example.test", "Admin");
            assertTrue(service.current().isEmpty());
            var first = new ExternalQuotePreviewPolicyService.Configuration(1, "BINANCE", "0.0020", "0.1",
                    Map.of("BTC", "0", "ETH", "-0.001"), Map.of("BTC", "-0.05"));
            assertEquals("0.002", transaction.execute(status -> service.publish(actor, first)).platformRate());
            assertEquals("0.1", service.current().orElseThrow().platformFixedUsdt());
            transaction.execute(status -> service.publish(actor, first));
            assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM external_quote_preview_policies", Integer.class));
            assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM audit_logs "
                    + "WHERE action_type='EXTERNAL_QUOTE_PREVIEW_POLICY_PUBLISHED'", Integer.class));
            assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM outbox_events "
                    + "WHERE event_type='EXTERNAL_QUOTE_PREVIEW_POLICY_PUBLISHED'", Integer.class));
            assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM orders", Integer.class));
            assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM ledger_journals", Integer.class));
            assertThrows(IllegalArgumentException.class, () -> transaction.execute(status -> service.publish(actor,
                    new ExternalQuotePreviewPolicyService.Configuration(1, "BINANCE", "0.003", "0", Map.of(), Map.of()))));
            assertThrows(IllegalArgumentException.class, () -> transaction.execute(status -> service.publish(actor,
                    new ExternalQuotePreviewPolicyService.Configuration(3, "BINANCE", "0.003", "0", Map.of(), Map.of()))));
            assertThrows(DataAccessException.class, () -> jdbc.update(
                    "UPDATE external_quote_preview_policies SET source='BINANCE' WHERE policy_version=1"));
            assertEquals(1, service.current().orElseThrow().version());
        }
    }
}
