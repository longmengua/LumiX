package com.lumix.ledger.application.posting.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lumix.account.AccountId;
import com.lumix.account.AssetSymbol;
import com.lumix.common.RequestId;
import com.lumix.ledger.application.DefaultLedgerRuntimeBoundary;
import com.lumix.ledger.application.LedgerRuntimePrerequisite;
import com.lumix.ledger.application.idempotency.LedgerIdempotencyDesignPolicy;
import com.lumix.ledger.application.posting.DefaultLedgerPostingCommandBoundary;
import com.lumix.ledger.application.posting.LedgerPostingCommand;
import com.lumix.ledger.application.posting.LedgerPostingCommandBoundary;
import com.lumix.ledger.application.posting.LedgerPostingCommandResult;
import com.lumix.ledger.application.posting.LedgerPostingDecision;
import com.lumix.ledger.application.posting.LedgerPostingPlan;
import com.lumix.ledger.domain.LedgerBusinessReferenceType;
import com.lumix.ledger.domain.LedgerDirection;
import com.lumix.ledger.domain.LedgerEntryDraft;
import com.lumix.ledger.domain.LedgerInvariantPolicy;
import com.lumix.ledger.domain.LedgerJournalDraft;
import com.lumix.ledger.persistence.DefaultLedgerAppendOnlyPersistenceMapper;
import com.lumix.ledger.persistence.LedgerAppendOnlyPersistenceMapping;
import com.lumix.ledger.persistence.LedgerEntryPersistenceMapping;
import com.lumix.ledger.persistence.LedgerJournalPersistenceMapping;
import com.lumix.ledger.persistence.adapter.LedgerConnectionProvider;
import com.lumix.ledger.persistence.adapter.LedgerAppendOnlyJdbcAdapter;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.flywaydb.core.Flyway;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;

/**
 * 驗證受控 ledger posting runtime gate 只接最小 append path，不會偷跑到完整 trading runtime。
 */
class LedgerPostingRuntimeGateTest {

    /**
     * 確認 valid command 經過受控 runtime gate 後只會 append ledger_journals 與 ledger_entries。
     *
     * 這個 case 必須存在，因為 P15-T04 的核心就是把 accepted plan 安全地送進 append adapter，但不能順便啟動其他 money movement runtime。
     */
    @Test
    void appendAcceptedCommandWritesLedgerTablesOnly() throws Exception {
        JdbcDataSource dataSource = createDataSource("jdbc:h2:mem:p15_t04_append;MODE=PostgreSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1");
        migrate(dataSource);
        seedBaseRowsWithBothAccountAssets(dataSource);

        LedgerPostingRuntimeGate gate = createGate(dataSource::getConnection);
        LedgerPostingCommand command = createCommand(
                "req-p15-t04-append-001",
                "order-p15-t04-001",
                new BigDecimal("42.000000"),
                new BigDecimal("42.000000")
        );

        LedgerPostingAppendResult result = gate.append(
                command,
                allPrerequisites(),
                "idem-p15-t04-append-001"
        );

        assertTrue(result.appendCompleted());
        assertEquals(LedgerPostingDecision.ACCEPTED, result.commandResult().decision());
        assertNotNull(result.ledgerJournalId());

        try (Connection connection = dataSource.getConnection()) {
            assertEquals(1L, countRows(connection, "ledger_journals"));
            assertEquals(2L, countRows(connection, "ledger_entries"));
            assertEquals(0L, countRows(connection, "balance_projections"));

            assertJournalRow(connection, result.ledgerJournalId());
            assertEntryRow(connection, result.ledgerJournalId(), 1L, "acct-debit", "USDT", "DEBIT", new BigDecimal("42.000000"));
            assertEntryRow(connection, result.ledgerJournalId(), 2L, "acct-credit", "USDT", "CREDIT", new BigDecimal("42.000000"));
        }
    }

    /**
     * 確認 prerequisite 不足時會 rejected，且不會 append 任何 ledger row。
     *
     * 這個 case 必須存在，因為 gate 必須先停在 prerequisite 層，不能把失敗的 command 往 DB 推。
     */
    @Test
    void rejectedPrerequisitesDoNotAppendAnything() throws Exception {
        JdbcDataSource dataSource = createDataSource("jdbc:h2:mem:p15_t04_rejected;MODE=PostgreSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1");
        migrate(dataSource);
        seedBaseRowsWithBothAccountAssets(dataSource);

        LedgerPostingRuntimeGate gate = createGate(dataSource::getConnection);

        LedgerPostingAppendResult result = gate.append(
                createCommand(
                        "req-p15-t04-rejected-001",
                        "order-p15-t04-rejected-001",
                        new BigDecimal("5.000000"),
                        new BigDecimal("5.000000")
                ),
                Set.of(),
                "idem-p15-t04-rejected-001"
        );

        assertFalse(result.appendCompleted());
        assertEquals(LedgerPostingDecision.REJECTED, result.commandResult().decision());

        try (Connection connection = dataSource.getConnection()) {
            assertEquals(0L, countRows(connection, "ledger_journals"));
            assertEquals(0L, countRows(connection, "ledger_entries"));
            assertEquals(0L, countRows(connection, "balance_projections"));
        }
    }

    /**
     * 確認 entry insert 失敗時 journal header 也會 rollback。
     *
     * 這個 case 必須存在，因為受控 gate 已經進到真正 DB append，atomicity 不可以壞掉。
     */
    @Test
    void rollbackWhenEntryInsertFails() throws Exception {
        JdbcDataSource dataSource = createDataSource("jdbc:h2:mem:p15_t04_rollback;MODE=PostgreSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1");
        migrate(dataSource);
        seedBaseRowsWithMissingCreditAccountAsset(dataSource);

        LedgerPostingRuntimeGate gate = createGate(dataSource::getConnection);

        assertThrows(IllegalStateException.class, () -> gate.append(
                createCommandWithCreditAccountId(
                        "req-p15-t04-rollback-001",
                        "order-p15-t04-rollback-001",
                        "acct-missing-credit",
                        new BigDecimal("7.500000"),
                        new BigDecimal("7.500000")
                ),
                allPrerequisites(),
                "idem-p15-t04-rollback-001"
        ));

        try (Connection connection = dataSource.getConnection()) {
            assertEquals(0L, countRows(connection, "ledger_journals"));
            assertEquals(0L, countRows(connection, "ledger_entries"));
            assertEquals(0L, countRows(connection, "balance_projections"));
        }
    }

    /**
     * 確認 mapping 既有 requestId 與 command requestId 衝突時會直接失敗，且不會 append 任何 ledger row。
     *
     * 這個 case 必須存在，因為 requestId 是 trace / audit linkage，不可被靜默覆蓋成另一個值。
     */
    @Test
    void requestIdConflictFailsBeforeAnyAppend() throws Exception {
        JdbcDataSource dataSource = createDataSource("jdbc:h2:mem:p15_t04_request_id_conflict;MODE=PostgreSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1");
        migrate(dataSource);

        LedgerPostingRuntimeGate gate = createConflictGate(dataSource::getConnection);

        assertThrows(IllegalStateException.class, () -> gate.append(
                createCommand(
                        "req-p15-t04-conflict-001",
                        "order-p15-t04-conflict-001",
                        new BigDecimal("11.000000"),
                        new BigDecimal("11.000000")
                ),
                allPrerequisites(),
                "idem-p15-t04-conflict-001"
        ));

        try (Connection connection = dataSource.getConnection()) {
            assertEquals(0L, countRows(connection, "ledger_journals"));
            assertEquals(0L, countRows(connection, "ledger_entries"));
            assertEquals(0L, countRows(connection, "balance_projections"));
        }
    }

    /**
     * 建立受控 runtime gate。
     *
     * 這裡只接最小 application boundary 與 append adapter，不接任何 balance / reservation / settlement runtime。
     */
    private static LedgerPostingRuntimeGate createGate(LedgerConnectionProvider connectionProvider) {
        DefaultLedgerRuntimeBoundary runtimeBoundary = new DefaultLedgerRuntimeBoundary();
        LedgerInvariantPolicy invariantPolicy = new LedgerInvariantPolicy();
        DefaultLedgerPostingCommandBoundary commandBoundary = new DefaultLedgerPostingCommandBoundary(
                runtimeBoundary,
                invariantPolicy,
                new DefaultLedgerAppendOnlyPersistenceMapper()
        );
        LedgerAppendOnlyJdbcAdapter appendOnlyJdbcAdapter = new LedgerAppendOnlyJdbcAdapter(connectionProvider);
        return new LedgerPostingRuntimeGate(
                runtimeBoundary,
                commandBoundary,
                appendOnlyJdbcAdapter,
                new LedgerIdempotencyDesignPolicy()
        );
    }

    /**
     * 建立會回傳 requestId 衝突 mapping 的受控 gate。
     *
     * 這個 gate 只用來驗證 requestId 不能被靜默覆蓋，並不代表正式 runtime 已完成。
     */
    private static LedgerPostingRuntimeGate createConflictGate(LedgerConnectionProvider connectionProvider) {
        DefaultLedgerRuntimeBoundary runtimeBoundary = new DefaultLedgerRuntimeBoundary();
        LedgerPostingCommandBoundary commandBoundary = (command, prerequisites) -> new LedgerPostingCommandResult(
                LedgerPostingDecision.ACCEPTED,
                new LedgerPostingPlan(
                        command,
                        new LedgerAppendOnlyPersistenceMapping(
                                new LedgerJournalPersistenceMapping(
                                        LedgerBusinessReferenceType.ORDER,
                                        command.journalDraft().businessReferenceId(),
                                        "req-existing-conflict",
                                        null,
                                        command.submittedAt()
                                ),
                                List.of(
                                        new LedgerEntryPersistenceMapping(
                                                java.util.Optional.empty(),
                                                1L,
                                                new AccountId("acct-debit"),
                                                new AssetSymbol("USDT"),
                                                LedgerDirection.DEBIT,
                                                new BigDecimal("11.000000")
                                        ),
                                        new LedgerEntryPersistenceMapping(
                                                java.util.Optional.empty(),
                                                2L,
                                                new AccountId("acct-credit"),
                                                new AssetSymbol("USDT"),
                                                LedgerDirection.CREDIT,
                                                new BigDecimal("11.000000")
                                        )
                                )
                        ),
                        List.of()
                ),
                null
        );
        LedgerAppendOnlyJdbcAdapter appendOnlyJdbcAdapter = new LedgerAppendOnlyJdbcAdapter(connectionProvider);
        return new LedgerPostingRuntimeGate(
                runtimeBoundary,
                commandBoundary,
                appendOnlyJdbcAdapter,
                new LedgerIdempotencyDesignPolicy()
        );
    }

    /**
     * 建立 H2 DataSource。
     *
     * 這裡只供 gate 測試，不代表正式 runtime 已完成。
     */
    private static JdbcDataSource createDataSource(String url) {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL(url);
        dataSource.setUser("sa");
        dataSource.setPassword("");
        return dataSource;
    }

    /**
     * 套用既有 Flyway migration。
     *
     * 這樣測試才能直接對齊 Phase 12 / Phase 14 的 schema contract。
     */
    private static void migrate(JdbcDataSource dataSource) {
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .cleanDisabled(false)
                .load();

        flyway.clean();
        flyway.migrate();
    }

    /**
     * 準備 valid runtime gate 所需的 identity / account / asset / account_assets 基礎資料。
     *
     * ledger gate 只處理受控接線，前置 boundary 資料仍必須由測試明確 seed。
     */
    private static void seedBaseRowsWithBothAccountAssets(JdbcDataSource dataSource) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            insertUser(connection, "user-p15-t04", "p15-t04@example.com");
            insertAccount(connection, "acct-debit", "user-p15-t04", "SPOT");
            insertAccount(connection, "acct-credit", "user-p15-t04", "MARGIN");
            insertAsset(connection, "USDT", "Tether USD");
            insertAccountAsset(connection, "acct-debit", "USDT");
            insertAccountAsset(connection, "acct-credit", "USDT");
        }
    }

    /**
     * 準備會在 entry insert 階段失敗的基礎資料。
     *
     * 這裡刻意少 seed credit side 的 account_assets，讓受控 gate 可以驗證 rollback。
     */
    private static void seedBaseRowsWithMissingCreditAccountAsset(JdbcDataSource dataSource) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            insertUser(connection, "user-p15-t04-rollback", "p15-t04-rollback@example.com");
            insertAccount(connection, "acct-debit", "user-p15-t04-rollback", "SPOT");
            insertAsset(connection, "USDT", "Tether USD");
            insertAccountAsset(connection, "acct-debit", "USDT");
        }
    }

    /**
     * 建立 posting command。
     *
     * 這裡只組最小 command，不把 gate 測試升級成完整交易流程。
     */
    private static LedgerPostingCommand createCommand(
            String requestId,
            String businessReferenceId,
            BigDecimal debitAmount,
            BigDecimal creditAmount
    ) {
        return new LedgerPostingCommand(
                new RequestId(requestId),
                new LedgerJournalDraft(
                        LedgerBusinessReferenceType.ORDER,
                        businessReferenceId,
                        List.of(
                                new LedgerEntryDraft(
                                        new AccountId("acct-debit"),
                                        new AssetSymbol("USDT"),
                                        LedgerDirection.DEBIT,
                                        debitAmount,
                                        1L
                                ),
                                new LedgerEntryDraft(
                                        new AccountId("acct-credit"),
                                        new AssetSymbol("USDT"),
                                        LedgerDirection.CREDIT,
                                        creditAmount,
                                        2L
                                )
                        )
                ),
                Instant.parse("2026-07-11T04:00:00Z")
        );
    }

    /**
     * 建立 credit account 可自訂的 posting command。
     *
     * 這個版本只用在 rollback 測試，方便刻意把 credit entry 指向不存在的 account 來觸發 DB FK failure。
     */
    private static LedgerPostingCommand createCommandWithCreditAccountId(
            String requestId,
            String businessReferenceId,
            String creditAccountId,
            BigDecimal debitAmount,
            BigDecimal creditAmount
    ) {
        return new LedgerPostingCommand(
                new RequestId(requestId),
                new LedgerJournalDraft(
                        LedgerBusinessReferenceType.ORDER,
                        businessReferenceId,
                        List.of(
                                new LedgerEntryDraft(
                                        new AccountId("acct-debit"),
                                        new AssetSymbol("USDT"),
                                        LedgerDirection.DEBIT,
                                        debitAmount,
                                        1L
                                ),
                                new LedgerEntryDraft(
                                        new AccountId(creditAccountId),
                                        new AssetSymbol("USDT"),
                                        LedgerDirection.CREDIT,
                                        creditAmount,
                                        2L
                                )
                        )
                ),
                Instant.parse("2026-07-11T04:00:00Z")
        );
    }

    /**
     * 取得完整 prerequisite 集合。
     *
     * 這裡只提供 gate 測試用的最小完整集，不代表正式 runtime 已上線。
     */
    private static Set<LedgerRuntimePrerequisite> allPrerequisites() {
        return EnumSet.allOf(LedgerRuntimePrerequisite.class);
    }

    /**
     * 插入測試使用者。
     *
     * 這是純 seed，不是正式 runtime。
     */
    private static void insertUser(Connection connection, String userId, String email) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO users (user_id, email, display_name, status) VALUES (?, ?, ?, ?)")) {
            statement.setString(1, userId);
            statement.setString(2, email);
            statement.setString(3, "Phase 15 Gate User");
            statement.setString(4, "ACTIVE");
            statement.executeUpdate();
        }
    }

    /**
     * 插入測試帳戶。
     *
     * 這是純 seed，不是正式 runtime。
     */
    private static void insertAccount(Connection connection, String accountId, String userId, String accountType)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO accounts (account_id, user_id, account_type, status) VALUES (?, ?, ?, ?)")) {
            statement.setString(1, accountId);
            statement.setString(2, userId);
            statement.setString(3, accountType);
            statement.setString(4, "ACTIVE");
            statement.executeUpdate();
        }
    }

    /**
     * 插入測試資產。
     *
     * 這是純 seed，不是正式 runtime。
     */
    private static void insertAsset(Connection connection, String assetSymbol, String displayName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO assets (asset_symbol, display_name, precision_scale, status) VALUES (?, ?, ?, ?)")) {
            statement.setString(1, assetSymbol);
            statement.setString(2, displayName);
            statement.setShort(3, (short) 6);
            statement.setString(4, "ACTIVE");
            statement.executeUpdate();
        }
    }

    /**
     * 插入 account_assets 關聯。
     *
     * ledger_entries 必須透過 account_id + asset_symbol 對齊 account_assets，所以測試要明確 seed。
     */
    private static void insertAccountAsset(Connection connection, String accountId, String assetSymbol) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO account_assets (account_id, asset_symbol, status) VALUES (?, ?, ?)")) {
            statement.setString(1, accountId);
            statement.setString(2, assetSymbol);
            statement.setString(3, "ACTIVE");
            statement.executeUpdate();
        }
    }

    /**
     * 讀取指定表的 row count。
     *
     * 這裡只做測試驗證，不代表正式 runtime 應該採用這種查詢模式。
     */
    private static long countRows(Connection connection, String tableName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM " + tableName);
                ResultSet resultSet = statement.executeQuery()) {
            if (!resultSet.next()) {
                throw new IllegalStateException("No row count returned for " + tableName);
            }
            return resultSet.getLong(1);
        }
    }

    /**
     * 驗證 journal header 已經 append。
     *
     * 這裡只檢查受控 gate 的 journal mapping，不把它誤解成完整 money movement。
     */
    private static void assertJournalRow(Connection connection, long ledgerJournalId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT business_reference_type, business_reference_id, request_id, journal_note, posted_at "
                        + "FROM ledger_journals WHERE ledger_journal_id = ?")) {
            statement.setLong(1, ledgerJournalId);

            try (ResultSet resultSet = statement.executeQuery()) {
                assertTrue(resultSet.next(), "ledger_journals row should exist");
                assertEquals("ORDER", resultSet.getString("business_reference_type"));
                assertEquals("order-p15-t04-001", resultSet.getString("business_reference_id"));
                assertEquals("req-p15-t04-append-001", resultSet.getString("request_id"));
                assertEquals(null, resultSet.getString("journal_note"));
                OffsetDateTime postedAt = resultSet.getObject("posted_at", OffsetDateTime.class);
                assertNotNull(postedAt);
                assertEquals(Instant.parse("2026-07-11T04:00:00Z"), postedAt.toInstant());
            }
        }
    }

    /**
     * 驗證 entry row 已經 append。
     *
     * 這裡只檢查 ledger_entries，不處理任何 balance mutation。
     */
    private static void assertEntryRow(
            Connection connection,
            long ledgerJournalId,
            long sequence,
            String accountId,
            String assetSymbol,
            String direction,
            BigDecimal amount
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT ledger_journal_id, entry_sequence, account_id, asset_symbol, direction, amount "
                        + "FROM ledger_entries WHERE ledger_journal_id = ? AND entry_sequence = ?")) {
            statement.setLong(1, ledgerJournalId);
            statement.setLong(2, sequence);

            try (ResultSet resultSet = statement.executeQuery()) {
                assertTrue(resultSet.next(), "ledger_entries row should exist for sequence " + sequence);
                assertEquals(ledgerJournalId, resultSet.getLong("ledger_journal_id"));
                assertEquals(sequence, resultSet.getLong("entry_sequence"));
                assertEquals(accountId, resultSet.getString("account_id"));
                assertEquals(assetSymbol, resultSet.getString("asset_symbol"));
                assertEquals(direction, resultSet.getString("direction"));
                assertEquals(0, amount.compareTo(resultSet.getBigDecimal("amount")));
                assertFalse(resultSet.next());
            }
        }
    }
}
