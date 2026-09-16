package com.lumix.admin.asset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;

import com.lumix.admin.superadmin.SuperAdminAccessService;
import com.lumix.ledger.domain.LedgerInvariantPolicy;
import com.lumix.ledger.runtime.LedgerBalanceProjectionUpdater;
import com.lumix.ledger.runtime.TransactionalLedgerPostingService;
import com.lumix.testing.JdbcDataSource;
import com.lumix.user.auth.domain.AuthenticatedUser;
import java.math.BigDecimal;
import java.sql.Connection;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** 以真實 PostgreSQL 驗證資產調整絕不直接改餘額，且同類型重送只產生一筆 immutable journal。 */
class AdminAirdropServiceIntegrationTest {

    @Test
    void adjustmentPostsDoubleEntryProjectsUserBalanceAndReplaysByType() throws Exception {
        JdbcDataSource dataSource = dataSource("admin_airdrop_service");
        migrate(dataSource);
        seed(dataSource);
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        SuperAdminAccessService access = mock(SuperAdminAccessService.class);
        AuthenticatedUser actor = new AuthenticatedUser("admin-user", "admin@example.com", "管理員");
        doNothing().when(access).requireActiveSuperAdmin(actor);
        TransactionalLedgerPostingService ledger = new TransactionalLedgerPostingService(
                jdbcTemplate, new LedgerInvariantPolicy(), new LedgerBalanceProjectionUpdater(jdbcTemplate));
        AdminAirdropService service = new AdminAirdropService(access, jdbcTemplate, ledger,
                Clock.fixed(Instant.parse("2026-09-15T00:00:00Z"), ZoneOffset.UTC));
        AdminAirdropCommand command = new AdminAirdropCommand(
                "target-user", "USDT", new BigDecimal("25.125000"), "AIRDROP", "新戶獎勵"
        );
        TransactionTemplate transaction = new TransactionTemplate(new DataSourceTransactionManager(dataSource));

        AdminAirdropResult first = transaction.execute(status -> service.grant(actor, command));
        AdminAirdropResult replay = transaction.execute(status -> service.grant(actor, command));

        assertFalse(first.replayed());
        assertTrue(replay.replayed());
        assertEquals(first.ledgerJournalId(), replay.ledgerJournalId());
        assertEquals(1L, count(jdbcTemplate, "ledger_journals"));
        assertEquals(2L, count(jdbcTemplate, "ledger_entries"));
        assertEquals(2L, count(jdbcTemplate, "audit_logs"));
        assertEquals(0, new BigDecimal("25.125000").compareTo(jdbcTemplate.queryForObject(
                "SELECT total_amount FROM balance_projections WHERE account_id = 'target-user:spot' AND asset_symbol = 'USDT'", BigDecimal.class)));
        assertEquals(0, new BigDecimal("25.125000").compareTo(jdbcTemplate.queryForObject(
                "SELECT available_amount FROM balance_projections WHERE account_id = 'target-user:spot' AND asset_symbol = 'USDT'", BigDecimal.class)));
        assertEquals(0L, count(jdbcTemplate, "balance_projections WHERE account_id = 'system:airdrop:spot'"));

        AdminAirdropResult reversal = transaction.execute(status -> service.grant(actor, new AdminAirdropCommand(
                "target-user", "USDT", new BigDecimal("-5.125000"), "REVERSAL", "更正重複入帳"
        )));
        assertFalse(reversal.replayed());
        assertEquals(2L, count(jdbcTemplate, "ledger_journals"));
        assertEquals(4L, count(jdbcTemplate, "ledger_entries"));
        assertEquals(0, new BigDecimal("20.000000").compareTo(jdbcTemplate.queryForObject(
                "SELECT available_amount FROM balance_projections WHERE account_id = 'target-user:spot' AND asset_symbol = 'USDT'", BigDecimal.class)));
        assertThrows(IllegalArgumentException.class, () -> transaction.execute(status -> service.grant(actor,
                new AdminAirdropCommand("target-user", "USDT", new BigDecimal("-20.000001"), "REVERSAL", "餘額不足測試"))));
        assertEquals(2L, count(jdbcTemplate, "ledger_journals"));

        AdminAirdropAssetConfigurationService configuration = new AdminAirdropAssetConfigurationService(access, jdbcTemplate);
        assertEquals(List.of("USDT"), configuration.listActiveSpotAssets(actor).stream()
                .map(AdminAirdropAssetOption::assetSymbol).toList());

        AdminSpotAssetConfigurationService spotConfiguration = new AdminSpotAssetConfigurationService(access, jdbcTemplate);
        AdminSpotAssetConfiguration createdAsset = transaction.execute(status ->
                spotConfiguration.create(actor, "BTC", "Bitcoin", 8));
        assertEquals("HALTED", createdAsset.status());
        AdminSpotAssetConfiguration activatedAsset = transaction.execute(status ->
                spotConfiguration.updateStatus(actor, "BTC", "ACTIVE"));
        assertEquals("ACTIVE", activatedAsset.status());
        assertEquals(List.of("BTC", "USDT"), spotConfiguration.list(actor).stream()
                .map(AdminSpotAssetConfiguration::assetSymbol).toList());
        assertEquals(6L, count(jdbcTemplate, "audit_logs"));
    }

    private static JdbcDataSource dataSource(String schema) {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:postgresql:test:" + schema);
        dataSource.setUser("ignored");
        dataSource.setPassword("ignored");
        return dataSource;
    }

    private static void migrate(JdbcDataSource dataSource) {
        Flyway flyway = Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").cleanDisabled(false).load();
        flyway.clean();
        flyway.migrate();
    }

    private static void seed(JdbcDataSource dataSource) throws Exception {
        try (Connection connection = dataSource.getConnection(); var statement = connection.createStatement()) {
            statement.executeUpdate("INSERT INTO users (user_id, email, display_name, status) VALUES "
                    + "('target-user', 'target@example.com', 'Target User', 'ACTIVE'), "
                    + "('admin-user', 'admin@example.com', 'Admin User', 'ACTIVE')");
            statement.executeUpdate("INSERT INTO accounts (account_id, user_id, account_type, status) VALUES "
                    + "('target-user:spot', 'target-user', 'SPOT', 'ACTIVE')");
            statement.executeUpdate("INSERT INTO assets (asset_symbol, display_name, precision_scale, status) "
                    + "VALUES ('USDT', 'Tether', 6, 'ACTIVE')");
        }
    }

    private static long count(JdbcTemplate jdbcTemplate, String relation) {
        Long value = jdbcTemplate.queryForObject("SELECT count(*) FROM " + relation, Long.class);
        return value == null ? 0L : value;
    }
}
