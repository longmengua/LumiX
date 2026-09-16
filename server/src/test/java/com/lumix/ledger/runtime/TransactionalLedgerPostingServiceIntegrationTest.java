package com.lumix.ledger.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lumix.account.AccountId;
import com.lumix.account.AssetSymbol;
import com.lumix.common.RequestId;
import com.lumix.ledger.application.posting.LedgerPostingCommand;
import com.lumix.ledger.domain.LedgerBusinessReferenceType;
import com.lumix.ledger.domain.LedgerDirection;
import com.lumix.ledger.domain.LedgerEntryDraft;
import com.lumix.ledger.domain.LedgerInvariantPolicy;
import com.lumix.ledger.domain.LedgerJournalDraft;
import com.lumix.testing.JdbcDataSource;
import com.lumix.reservation.ReservationHoldCommand;
import com.lumix.reservation.ReservationHoldService;
import com.lumix.reservation.ReservationReleaseService;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** 直接以 PostgreSQL 驗證資產帳本入帳的 transaction 邊界，不使用 mock database 或 fake adapter。 */
class TransactionalLedgerPostingServiceIntegrationTest {

    /**
     * 成功入帳與重送必須在真實 PostgreSQL 分別留下唯一的一組 immutable evidence，不能只靠 unit mock 證明。
     */
    @Test
    void postAndReplayPersistOneCompleteEvidenceSet() throws Exception {
        JdbcDataSource dataSource = dataSource("ledger_posting_service");
        migrate(dataSource);
        seedReferences(dataSource);
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        TransactionTemplate transaction = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        TransactionalLedgerPostingService service = new TransactionalLedgerPostingService(
                jdbcTemplate, new LedgerInvariantPolicy(), new LedgerBalanceProjectionUpdater(jdbcTemplate));
        LedgerPostingExecutionCommand command = command();

        LedgerPostingExecutionResult first = transaction.execute(status -> service.post(command));
        LedgerPostingExecutionResult replay = transaction.execute(status -> service.post(command));
        ReservationHoldService reservationHoldService = new ReservationHoldService(jdbcTemplate);
        transaction.execute(status -> reservationHoldService.hold(new ReservationHoldCommand(
                "reservation-runtime-001", "ledger-credit", "USDT", "transfer-runtime-001",
                new BigDecimal("2.000000"), "request-reservation-runtime-001",
                "reservation-runtime-idempotency-001", "asset-runtime"
        )));
        assertEquals(0, new BigDecimal("10.345678").compareTo(jdbcTemplate.queryForObject(
                "SELECT available_amount FROM balance_projections WHERE account_id = 'ledger-credit' AND asset_symbol = 'USDT'", BigDecimal.class)));
        new TransactionTemplate(new DataSourceTransactionManager(dataSource)).execute(status -> {
            new ReservationReleaseService(jdbcTemplate).release("reservation-runtime-001", "request-reservation-release-001",
                    "reservation-release-idempotency-001", "asset-runtime");
            return null;
        });

        assertEquals(first.ledgerJournalId(), replay.ledgerJournalId());
        assertFalse(first.replayed());
        assertTrue(replay.replayed());
        assertEquals(1L, count(jdbcTemplate, "ledger_journals"));
        assertEquals(2L, count(jdbcTemplate, "ledger_entries"));
        assertEquals(3L, count(jdbcTemplate, "idempotency_keys"));
        assertEquals(1L, count(jdbcTemplate, "outbox_events"));
        assertEquals(3L, count(jdbcTemplate, "audit_logs"));
        assertEquals(1L, count(jdbcTemplate, "reservations"));
        BigDecimal projectedTotal = jdbcTemplate.queryForObject(
                "SELECT total_amount FROM balance_projections WHERE account_id = 'ledger-credit' AND asset_symbol = 'USDT'",
                BigDecimal.class);
        assertEquals(0, new BigDecimal("12.345678").compareTo(projectedTotal));
        assertEquals(0, new BigDecimal("12.345678").compareTo(jdbcTemplate.queryForObject(
                "SELECT available_amount FROM balance_projections WHERE account_id = 'ledger-credit' AND asset_symbol = 'USDT'",
                BigDecimal.class)));
        assertEquals(0, BigDecimal.ZERO.compareTo(jdbcTemplate.queryForObject(
                "SELECT locked_amount FROM balance_projections WHERE account_id = 'ledger-credit' AND asset_symbol = 'USDT'",
                BigDecimal.class)));
        assertEquals("RELEASED", jdbcTemplate.queryForObject(
                "SELECT status FROM reservations WHERE reservation_id = 'reservation-runtime-001'", String.class));
    }

    private static JdbcDataSource dataSource(String schema) {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:postgresql:test:" + schema);
        dataSource.setUser("ignored");
        dataSource.setPassword("ignored");
        return dataSource;
    }

    private static void migrate(JdbcDataSource dataSource) {
        Flyway flyway = Flyway.configure().dataSource(dataSource).locations("classpath:db/migration")
                .cleanDisabled(false).load();
        flyway.clean();
        flyway.migrate();
    }

    private static void seedReferences(JdbcDataSource dataSource) throws SQLException {
        try (Connection connection = dataSource.getConnection(); var statement = connection.createStatement()) {
            statement.executeUpdate("INSERT INTO users (user_id, email, display_name, status) VALUES "
                    + "('ledger-user', 'ledger@example.com', 'Ledger User', 'ACTIVE')");
            statement.executeUpdate("INSERT INTO accounts (account_id, user_id, account_type, status) VALUES "
                    + "('ledger-debit', 'ledger-user', 'SPOT', 'ACTIVE'), "
                    + "('ledger-credit', 'ledger-user', 'MARGIN', 'ACTIVE')");
            statement.executeUpdate("INSERT INTO assets (asset_symbol, display_name, precision_scale, status) "
                    + "VALUES ('USDT', 'Tether', 6, 'ACTIVE')");
            statement.executeUpdate("INSERT INTO account_assets (account_id, asset_symbol, status) VALUES "
                    + "('ledger-debit', 'USDT', 'ACTIVE'), ('ledger-credit', 'USDT', 'ACTIVE')");
            statement.executeUpdate("UPDATE accounts SET account_category = 'EXCHANGE' WHERE account_id = 'ledger-debit'");
        }
    }

    private static LedgerPostingExecutionCommand command() {
        return new LedgerPostingExecutionCommand(
                new LedgerPostingCommand(
                        new RequestId("request-ledger-runtime-001"),
                        new LedgerJournalDraft(LedgerBusinessReferenceType.ADJUSTMENT, "ledger-runtime-001", List.of(
                                new LedgerEntryDraft(new AccountId("ledger-debit"), new AssetSymbol("USDT"),
                                        LedgerDirection.DEBIT, new BigDecimal("12.345678"), 1),
                                new LedgerEntryDraft(new AccountId("ledger-credit"), new AssetSymbol("USDT"),
                                        LedgerDirection.CREDIT, new BigDecimal("12.345678"), 2)
                        )),
                        Instant.parse("2026-09-15T00:00:00Z")
                ),
                "ledger-runtime-idempotency-001",
                new LedgerPostingActor("SYSTEM", "asset-runtime")
        );
    }

    private static long count(JdbcTemplate jdbcTemplate, String tableName) {
        Long count = jdbcTemplate.queryForObject("SELECT count(*) FROM " + tableName, Long.class);
        return count == null ? 0L : count;
    }
}
