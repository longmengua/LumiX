package com.lumix.account.runtime;

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
import com.lumix.ledger.runtime.LedgerBalanceProjectionUpdater;
import com.lumix.ledger.runtime.LedgerPostingActor;
import com.lumix.ledger.runtime.LedgerPostingExecutionCommand;
import com.lumix.ledger.runtime.TransactionalLedgerPostingService;
import com.lumix.reservation.ReservationCaptureService;
import com.lumix.reservation.ReservationHoldService;
import com.lumix.testing.JdbcDataSource;
import com.lumix.user.auth.domain.AuthenticatedUser;
import java.math.BigDecimal;
import java.sql.Connection;
import java.time.Instant;
import java.util.List;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** 真實 PostgreSQL 證明劃轉的 reservation、ledger 與兩端投影是同一個原子資產路徑。 */
class InternalTransferServiceIntegrationTest {
    @Test
    void transferHoldsCapturesPostsAndReplaysWithoutDuplicateMoneyMovement() throws Exception {
        JdbcDataSource source = source("internal_transfer_service"); migrate(source); seed(source);
        JdbcTemplate jdbc = new JdbcTemplate(source);
        TransactionalLedgerPostingService ledger = new TransactionalLedgerPostingService(jdbc, new LedgerInvariantPolicy(), new LedgerBalanceProjectionUpdater(jdbc));
        TransactionTemplate transaction = new TransactionTemplate(new DataSourceTransactionManager(source));
        transaction.execute(status -> ledger.post(new LedgerPostingExecutionCommand(new LedgerPostingCommand(new RequestId("seed-transfer-balance"),
                new LedgerJournalDraft(LedgerBusinessReferenceType.ADJUSTMENT, "seed-transfer-balance", List.of(
                        new LedgerEntryDraft(new AccountId("exchange-source"), new AssetSymbol("USDT"), LedgerDirection.DEBIT, new BigDecimal("100"), 1),
                        new LedgerEntryDraft(new AccountId("transfer-user:spot"), new AssetSymbol("USDT"), LedgerDirection.CREDIT, new BigDecimal("100"), 2)
                )), Instant.parse("2026-09-16T00:00:00Z")), "seed-transfer-balance-key", new LedgerPostingActor("SYSTEM", "test"))));
        InternalTransferService service = new InternalTransferService(jdbc, new ReservationHoldService(jdbc), new ReservationCaptureService(jdbc), ledger);
        AuthenticatedUser user = new AuthenticatedUser("transfer-user", "transfer@example.com", "Transfer User");

        InternalTransferService.InternalTransferResult first = transaction.execute(status -> service.transfer(user, "SPOT", "FUTURES", "USDT", new BigDecimal("20"), "transfer-key-001"));
        InternalTransferService.InternalTransferResult replay = transaction.execute(status -> service.transfer(user, "SPOT", "FUTURES", "USDT", new BigDecimal("20"), "transfer-key-001"));

        assertFalse(first.replayed()); assertTrue(replay.replayed()); assertEquals(first.ledgerJournalId(), replay.ledgerJournalId());
        assertAmount(jdbc, "transfer-user:spot", "80", "80", "0");
        assertAmount(jdbc, "transfer-user:futures", "20", "20", "0");
        assertEquals("CONSUMED", jdbc.queryForObject("SELECT status FROM reservations", String.class));
        assertEquals(2L, count(jdbc, "ledger_journals"));
    }

    /** 平台內部轉出只能由來源使用者的現貨帳戶扣款，且相同鍵回放不可重複入帳給收款人。 */
    @Test
    void platformInternalTransferMovesSpotBalanceToRecipientAndReplaysSafely() throws Exception {
        JdbcDataSource source = source("platform_internal_transfer_service"); migrate(source); seed(source);
        JdbcTemplate jdbc = new JdbcTemplate(source);
        TransactionalLedgerPostingService ledger = new TransactionalLedgerPostingService(jdbc, new LedgerInvariantPolicy(), new LedgerBalanceProjectionUpdater(jdbc));
        TransactionTemplate transaction = new TransactionTemplate(new DataSourceTransactionManager(source));
        transaction.execute(status -> ledger.post(new LedgerPostingExecutionCommand(new LedgerPostingCommand(new RequestId("seed-platform-transfer-balance"),
                new LedgerJournalDraft(LedgerBusinessReferenceType.ADJUSTMENT, "seed-platform-transfer-balance", List.of(
                        new LedgerEntryDraft(new AccountId("exchange-source"), new AssetSymbol("USDT"), LedgerDirection.DEBIT, new BigDecimal("100"), 1),
                        new LedgerEntryDraft(new AccountId("transfer-user:spot"), new AssetSymbol("USDT"), LedgerDirection.CREDIT, new BigDecimal("100"), 2)
                )), Instant.parse("2026-09-16T00:00:00Z")), "seed-platform-transfer-balance-key", new LedgerPostingActor("SYSTEM", "test"))));
        PlatformInternalTransferService service = new PlatformInternalTransferService(jdbc, new ReservationHoldService(jdbc), new ReservationCaptureService(jdbc), ledger);
        AuthenticatedUser user = new AuthenticatedUser("transfer-user", "transfer@example.com", "Transfer User");

        PlatformInternalTransferService.PlatformInternalTransferResult first = transaction.execute(status -> service.transfer(user, "receiver-user", "USDT", new BigDecimal("20"), "platform-transfer-key-001"));
        PlatformInternalTransferService.PlatformInternalTransferResult replay = transaction.execute(status -> service.transfer(user, "receiver-user", "USDT", new BigDecimal("20"), "platform-transfer-key-001"));

        assertFalse(first.replayed()); assertTrue(replay.replayed()); assertEquals(first.ledgerJournalId(), replay.ledgerJournalId());
        assertAmount(jdbc, "transfer-user:spot", "80", "80", "0");
        assertAmount(jdbc, "receiver-user:spot", "20", "20", "0");
        assertEquals("CONSUMED", jdbc.queryForObject("SELECT status FROM reservations WHERE reservation_id LIKE 'platform-transfer:%'", String.class));
        assertEquals(1L, jdbc.queryForObject("SELECT count(*) FROM audit_logs WHERE action_type = 'PLATFORM_INTERNAL_TRANSFER'", Long.class));
    }
    private static JdbcDataSource source(String schema) { JdbcDataSource source = new JdbcDataSource(); source.setURL("jdbc:postgresql:test:" + schema); source.setUser("ignored"); source.setPassword("ignored"); return source; }
    private static void migrate(JdbcDataSource source) { Flyway flyway = Flyway.configure().dataSource(source).locations("classpath:db/migration").cleanDisabled(false).load(); flyway.clean(); flyway.migrate(); }
    private static void seed(JdbcDataSource source) throws Exception { try (Connection connection = source.getConnection(); var statement = connection.createStatement()) {
        statement.executeUpdate("INSERT INTO users (user_id, email, display_name, status) VALUES ('transfer-user', 'transfer@example.com', 'Transfer User', 'ACTIVE'), ('receiver-user', 'receiver@example.com', 'Receiver User', 'ACTIVE'), ('exchange-user', 'exchange@example.com', 'Exchange', 'SUSPENDED')");
        statement.executeUpdate("INSERT INTO accounts (account_id, user_id, account_type, status, account_category) VALUES ('transfer-user:spot', 'transfer-user', 'SPOT', 'ACTIVE', 'USER'), ('transfer-user:futures', 'transfer-user', 'FUTURES', 'ACTIVE', 'USER'), ('receiver-user:spot', 'receiver-user', 'SPOT', 'ACTIVE', 'USER'), ('exchange-source', 'exchange-user', 'SPOT', 'ACTIVE', 'EXCHANGE')");
        statement.executeUpdate("INSERT INTO assets (asset_symbol, display_name, precision_scale, status) VALUES ('USDT', 'Tether', 6, 'ACTIVE')");
        statement.executeUpdate("INSERT INTO account_assets (account_id, asset_symbol, status) VALUES ('transfer-user:spot', 'USDT', 'ACTIVE'), ('exchange-source', 'USDT', 'ACTIVE')");
    }}
    private static void assertAmount(JdbcTemplate jdbc, String account, String total, String available, String locked) {
        List<BigDecimal> amounts = jdbc.query("SELECT total_amount, available_amount, locked_amount FROM balance_projections WHERE account_id = ? AND asset_symbol = 'USDT'", (row, number) -> List.of(row.getBigDecimal(1), row.getBigDecimal(2), row.getBigDecimal(3)), account).getFirst();
        assertEquals(0, new BigDecimal(total).compareTo(amounts.get(0))); assertEquals(0, new BigDecimal(available).compareTo(amounts.get(1))); assertEquals(0, new BigDecimal(locked).compareTo(amounts.get(2)));
    }
    private static long count(JdbcTemplate jdbc, String table) { Long count = jdbc.queryForObject("SELECT count(*) FROM " + table, Long.class); return count == null ? 0 : count; }
}
