package com.lumix.ledger.persistence.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lumix.account.AccountId;
import com.lumix.account.AssetSymbol;
import com.lumix.ledger.domain.LedgerBusinessReferenceType;
import com.lumix.ledger.domain.LedgerDirection;
import com.lumix.ledger.domain.LedgerEntryDraft;
import com.lumix.ledger.domain.LedgerJournalDraft;
import com.lumix.ledger.persistence.DefaultLedgerAppendOnlyPersistenceMapper;
import com.lumix.ledger.persistence.LedgerAppendOnlyPersistenceMapping;
import com.lumix.ledger.persistence.LedgerJournalPersistenceMapping;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import org.flywaydb.core.Flyway;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;

/**
 * 驗證最小 ledger append persistence adapter 只 append journal / entry，不會升級成正式 posting runtime。
 */
class LedgerAppendOnlyJdbcAdapterTest {

    /**
     * 確認 adapter 真的只 append ledger_journals 與 ledger_entries。
     *
     * <p>這個 case 必須存在，因為 Phase 14-T06 的責任是驗證 append-only gate，
     * 而不是把 adapter 接進 posting boundary。</p>
     */
    @Test
    void appendOnlyAdapterWritesJournalAndEntries() throws Exception {
        JdbcDataSource dataSource = createDataSource(
                "jdbc:h2:mem:p14_t06_append;MODE=PostgreSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1");
        migrate(dataSource);
        seedBaseRows(dataSource);

        LedgerAppendOnlyJdbcAdapter adapter = new LedgerAppendOnlyJdbcAdapter(dataSource::getConnection);
        LedgerAppendOnlyPersistenceMapping mapping = new DefaultLedgerAppendOnlyPersistenceMapper().describeAppendOnlyMapping(
                new LedgerJournalDraft(
                        LedgerBusinessReferenceType.ORDER,
                        "order-p14-t06-001",
                        List.of(
                                new LedgerEntryDraft(
                                        new AccountId("acct-debit"),
                                        new AssetSymbol("USDT"),
                                        LedgerDirection.DEBIT,
                                        new BigDecimal("150.25"),
                                        1L
                                ),
                                new LedgerEntryDraft(
                                        new AccountId("acct-credit"),
                                        new AssetSymbol("USDT"),
                                        LedgerDirection.CREDIT,
                                        new BigDecimal("150.25"),
                                        2L
                                )
                        )
                ),
                Instant.parse("2026-07-08T02:00:00Z")
        );

        long ledgerJournalId = adapter.append(mapping);

        try (Connection connection = dataSource.getConnection()) {
            assertEquals(1L, countRows(connection, "ledger_journals"));
            assertEquals(2L, countRows(connection, "ledger_entries"));
            assertEquals(0L, countRows(connection, "balance_projections"));

            assertJournalRow(connection, ledgerJournalId);
            assertEntryRow(connection, ledgerJournalId, 1L, "acct-debit", "USDT", "DEBIT", new BigDecimal("150.25"));
            assertEntryRow(connection, ledgerJournalId, 2L, "acct-credit", "USDT", "CREDIT", new BigDecimal("150.25"));
        }
    }

    /**
     * 確認 entry append 失敗時，journal header 也會 rollback。
     *
     * <p>這是 P14-T06 的必要測試，避免 ledger_journals 留下沒有完整 entries 的孤兒資料。</p>
     */
    @Test
    void rollbackWhenEntryInsertFails() throws Exception {
        JdbcDataSource dataSource = createDataSource(
                "jdbc:h2:mem:p14_t06_rollback;MODE=PostgreSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1");
        migrate(dataSource);
        seedBaseRows(dataSource);

        LedgerAppendOnlyJdbcAdapter adapter = new LedgerAppendOnlyJdbcAdapter(dataSource::getConnection);
        LedgerAppendOnlyPersistenceMapping mapping = new DefaultLedgerAppendOnlyPersistenceMapper().describeAppendOnlyMapping(
                new LedgerJournalDraft(
                        LedgerBusinessReferenceType.FEE,
                        "fee-p14-t06-rollback",
                        List.of(
                                new LedgerEntryDraft(
                                        new AccountId("acct-debit"),
                                        new AssetSymbol("USDT"),
                                        LedgerDirection.DEBIT,
                                        new BigDecimal("9.50"),
                                        1L
                                ),
                                new LedgerEntryDraft(
                                        new AccountId("acct-credit"),
                                        new AssetSymbol("BTC"),
                                        LedgerDirection.CREDIT,
                                        new BigDecimal("9.50"),
                                        2L
                                )
                        )
                ),
                Instant.parse("2026-07-08T03:00:00Z")
        );

        assertThrows(IllegalStateException.class, () -> adapter.append(mapping));

        try (Connection connection = dataSource.getConnection()) {
            assertEquals(0L, countRows(connection, "ledger_journals"));
            assertEquals(0L, countRows(connection, "ledger_entries"));
            assertEquals(0L, countRows(connection, "balance_projections"));
        }
    }

    /**
     * 確認 mapping contract 本身不允許空 entries。
     *
     * <p>adapter 也保留 defensive check，但不應為了測 adapter 而放寬 P14-T03 的 mapping contract。</p>
     */
    @Test
    void appendRejectsEmptyEntriesBeforeAnyInsert() throws Exception {
        JdbcDataSource dataSource = createDataSource(
                "jdbc:h2:mem:p14_t06_empty;MODE=PostgreSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1");
        migrate(dataSource);

        LedgerJournalPersistenceMapping journal = new LedgerJournalPersistenceMapping(
                LedgerBusinessReferenceType.ADJUSTMENT,
                "adj-p14-t06-empty",
                null,
                null,
                Instant.parse("2026-07-08T03:30:00Z")
        );

        LedgerAppendOnlyPersistenceMapping emptyMapping = new LedgerAppendOnlyPersistenceMapping(journal, List.of());

        LedgerAppendOnlyJdbcAdapter adapter = new LedgerAppendOnlyJdbcAdapter(dataSource::getConnection);
        assertThrows(IllegalArgumentException.class, () -> adapter.append(emptyMapping));

        try (Connection connection = dataSource.getConnection()) {
            assertEquals(0L, countRows(connection, "ledger_journals"));
            assertEquals(0L, countRows(connection, "ledger_entries"));
            assertEquals(0L, countRows(connection, "balance_projections"));
        }
    }

    /**
     * 確認 adapter 不會接受 null mapping。
     *
     * <p>這是最外層 defensive guard，避免測試或未來接線時傳入空 mapping。</p>
     */
    @Test
    void appendRejectsNullMapping() {
        JdbcDataSource dataSource = createDataSource(
                "jdbc:h2:mem:p14_t06_null;MODE=PostgreSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1");
        LedgerAppendOnlyJdbcAdapter adapter = new LedgerAppendOnlyJdbcAdapter(dataSource::getConnection);

        assertThrows(NullPointerException.class, () -> adapter.append(null));
    }

    /**
     * 確認 adapter 收到包含 null entry 的 mapping 時會 rollback 已寫入的 journal header。
     *
     * <p>這個 case 保護 RuntimeException 路徑，避免 setAutoCommit 還原時意外留下孤兒 journal。</p>
     */
    @Test
    void rollbackWhenRuntimeExceptionHappensAfterJournalInsert() throws Exception {
        JdbcDataSource dataSource = createDataSource(
                "jdbc:h2:mem:p14_t06_runtime_rollback;MODE=PostgreSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1");
        migrate(dataSource);
        seedBaseRows(dataSource);

        LedgerAppendOnlyJdbcAdapter adapter = new LedgerAppendOnlyJdbcAdapter(dataSource::getConnection);
        LedgerAppendOnlyPersistenceMapping mapping = new DefaultLedgerAppendOnlyPersistenceMapper().describeAppendOnlyMapping(
                new LedgerJournalDraft(
                        LedgerBusinessReferenceType.ADJUSTMENT,
                        "adj-p14-t06-runtime-rollback",
                        List.of(
                                new LedgerEntryDraft(new AccountId("acct-debit"), new AssetSymbol("USDT"), LedgerDirection.DEBIT,
                                        new BigDecimal("1.00"), 1L),
                                new LedgerEntryDraft(new AccountId("acct-credit"), new AssetSymbol("BTC"), LedgerDirection.CREDIT,
                                        new BigDecimal("1.00"), 2L)
                        )
                ),
                Instant.parse("2026-07-08T03:45:00Z")
        );

        assertThrows(IllegalStateException.class, () -> adapter.append(mapping));

        try (Connection connection = dataSource.getConnection()) {
            assertEquals(0L, countRows(connection, "ledger_journals"));
            assertEquals(0L, countRows(connection, "ledger_entries"));
            assertEquals(0L, countRows(connection, "balance_projections"));
        }
    }

    /**
     * 建立測試用 H2 DataSource。
     *
     * <p>這裡只為 adapter gate 準備測試環境，不代表正式 runtime 已完成。</p>
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
     * <p>這樣測試才會真的對齊 ledger_journals 與 ledger_entries 的 schema contract。</p>
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
     * 準備 ledger append 所需的 identity / account / asset / account_assets 基礎資料。
     *
     * <p>ledger adapter 只負責 append，不負責建立前置 boundary 資料。</p>
     */
    private static void seedBaseRows(JdbcDataSource dataSource) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            insertUser(connection, "user-p14-t06", "p14-t06@example.com");
            insertAccount(connection, "acct-debit", "user-p14-t06", "SPOT");
            insertAccount(connection, "acct-credit", "user-p14-t06", "MARGIN");
            insertAsset(connection, "USDT", "Tether USD");
            insertAccountAsset(connection, "acct-debit", "USDT");
            insertAccountAsset(connection, "acct-credit", "USDT");
        }
    }

    /**
     * 插入測試用使用者。
     *
     * <p>這個方法只做 seed，不是正式 runtime。</p>
     */
    private static void insertUser(Connection connection, String userId, String email) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO users (user_id, email, display_name, status) VALUES (?, ?, ?, ?)")) {
            statement.setString(1, userId);
            statement.setString(2, email);
            statement.setString(3, "Phase 14 Test User");
            statement.setString(4, "ACTIVE");
            statement.executeUpdate();
        }
    }

    /**
     * 插入測試用帳戶。
     *
     * <p>這個方法只做 seed，不是正式 runtime。</p>
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
     * 插入測試用資產。
     *
     * <p>這個方法只做 seed，不是正式 runtime。</p>
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
     * 插入測試用 account-asset 關聯。
     *
     * <p>ledger_entries 應透過 account_id + asset_symbol 對齊 account_assets，
     * 測試必須明確 seed，避免 FK 行為被誤判。</p>
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
     * <p>這個方法只用在測試，並且呼叫端只傳入固定 table name；
     * production adapter 不得使用這種 table name 串接方式。</p>
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
     * 驗證 journal header 已正確 append。
     *
     * <p>這裡只驗證欄位映射，不把 adapter 升級成 posting runtime。</p>
     */
    private static void assertJournalRow(Connection connection, long ledgerJournalId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT business_reference_type, business_reference_id, request_id, journal_note, posted_at "
                        + "FROM ledger_journals WHERE ledger_journal_id = ?")) {
            statement.setLong(1, ledgerJournalId);

            try (ResultSet resultSet = statement.executeQuery()) {
                assertTrue(resultSet.next(), "ledger_journals row should exist");
                assertEquals("ORDER", resultSet.getString("business_reference_type"));
                assertEquals("order-p14-t06-001", resultSet.getString("business_reference_id"));
                assertNull(resultSet.getString("request_id"));
                assertNull(resultSet.getString("journal_note"));

                OffsetDateTime postedAt = resultSet.getObject("posted_at", OffsetDateTime.class);
                assertNotNull(postedAt);
                assertEquals(Instant.parse("2026-07-08T02:00:00Z"), postedAt.toInstant());
            }
        }
    }

    /**
     * 驗證 entry row 只 append 到 ledger_entries。
     *
     * <p>這個方法只檢查 DB append 結果，不處理任何 balance mutation。</p>
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
