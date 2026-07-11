package com.lumix.ledger.persistence.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;

/**
 * 驗證 ledger append adapter 可在 PostgreSQL 16 replay 與執行。
 *
 * <p>這個測試只在提供 PostgreSQL 連線資訊時執行，避免把本地 H2 回歸與 PostgreSQL verification 混在一起。</p>
 */
class LedgerAppendOnlyPostgresVerificationTest {

    private static final String JDBC_URL_PROPERTY = "lumix.postgres.jdbc-url";
    private static final String JDBC_USER_PROPERTY = "lumix.postgres.username";
    private static final String JDBC_PASSWORD_PROPERTY = "lumix.postgres.password";

    /**
     * 確認 PostgreSQL 16 replay 後，valid ledger mapping 可以 append 進 ledger tables。
     *
     * <p>這個 case 必須存在，因為 H2 通過不代表 PostgreSQL 行為完全一致。</p>
     */
    @Test
    void postgresAppendVerificationSucceeds() throws Exception {
        DataSource dataSource = createPostgresDataSource();
        Assumptions.assumeTrue(dataSource != null, "PostgreSQL connection properties are not provided.");

        migrate(dataSource);
        seedBaseRows(dataSource);

        LedgerAppendOnlyJdbcAdapter adapter = new LedgerAppendOnlyJdbcAdapter(((PGSimpleDataSource) dataSource)::getConnection);
        LedgerAppendOnlyPersistenceMapping mapping = new DefaultLedgerAppendOnlyPersistenceMapper().describeAppendOnlyMapping(
                new LedgerJournalDraft(
                        LedgerBusinessReferenceType.ORDER,
                        "pg-order-p14-t07-001",
                        List.of(
                                new LedgerEntryDraft(new AccountId("pg-acct-debit"), new AssetSymbol("USDT"), LedgerDirection.DEBIT,
                                        new BigDecimal("12.500000"), 1L),
                                new LedgerEntryDraft(new AccountId("pg-acct-credit"), new AssetSymbol("USDT"), LedgerDirection.CREDIT,
                                        new BigDecimal("12.500000"), 2L)
                        )
                ),
                Instant.parse("2026-07-08T04:00:00Z")
        );

        long ledgerJournalId = adapter.append(mapping);

        try (Connection connection = dataSource.getConnection()) {
            assertEquals(1L, countRows(connection, "ledger_journals"));
            assertEquals(2L, countRows(connection, "ledger_entries"));
            assertEquals(0L, countRows(connection, "balance_projections"));

            assertJournalRow(connection, ledgerJournalId);
            assertEntryRow(connection, ledgerJournalId, 1L, "pg-acct-debit", "USDT", "DEBIT", new BigDecimal("12.500000"));
            assertEntryRow(connection, ledgerJournalId, 2L, "pg-acct-credit", "USDT", "CREDIT", new BigDecimal("12.500000"));
        }
    }

    /**
     * 確認 PostgreSQL 下 entry insert 失敗時會 rollback，journal 與 entries 都不會殘留。
     *
     * <p>這個 case 必須存在，因為 append-only gate 需要在 PostgreSQL 上證明 atomicity。</p>
     */
    @Test
    void postgresRollbackVerificationKeepsLedgerTablesEmpty() throws Exception {
        DataSource dataSource = createPostgresDataSource();
        Assumptions.assumeTrue(dataSource != null, "PostgreSQL connection properties are not provided.");

        migrate(dataSource);
        seedBaseRows(dataSource);

        LedgerAppendOnlyJdbcAdapter adapter = new LedgerAppendOnlyJdbcAdapter(((PGSimpleDataSource) dataSource)::getConnection);
        LedgerAppendOnlyPersistenceMapping mapping = new DefaultLedgerAppendOnlyPersistenceMapper().describeAppendOnlyMapping(
                new LedgerJournalDraft(
                        LedgerBusinessReferenceType.ADJUSTMENT,
                        "pg-adj-p14-t07-rollback",
                        List.of(
                                new LedgerEntryDraft(new AccountId("pg-acct-debit"), new AssetSymbol("USDT"), LedgerDirection.DEBIT,
                                        new BigDecimal("1.000000"), 1L),
                                new LedgerEntryDraft(new AccountId("pg-acct-credit"), new AssetSymbol("BTC"), LedgerDirection.CREDIT,
                                        new BigDecimal("1.000000"), 2L)
                        )
                ),
                Instant.parse("2026-07-08T04:15:00Z")
        );

        assertThrows(IllegalStateException.class, () -> adapter.append(mapping));

        try (Connection connection = dataSource.getConnection()) {
            assertEquals(0L, countRows(connection, "ledger_journals"));
            assertEquals(0L, countRows(connection, "ledger_entries"));
            assertEquals(0L, countRows(connection, "balance_projections"));
        }
    }

    private static DataSource createPostgresDataSource() {
        String url = System.getProperty(JDBC_URL_PROPERTY);
        if (url == null || url.isBlank()) {
            return null;
        }

        String username = System.getProperty(JDBC_USER_PROPERTY, "postgres");
        String password = System.getProperty(JDBC_PASSWORD_PROPERTY, "");

        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setUrl(url);
        dataSource.setUser(username);
        dataSource.setPassword(password);
        return dataSource;
    }

    /**
     * 讓 PostgreSQL schema 重新 replay V001 到 V008。
     *
     * <p>這裡只驗證 migration 可回放，不替正式 runtime 加任何額外配置。</p>
     */
    private static void migrate(DataSource dataSource) {
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .cleanDisabled(false)
                .load();

        flyway.clean();
        flyway.migrate();
    }

    /**
     * 準備 PostgreSQL verification 所需的 identity / account / asset / account_assets 基礎資料。
     *
     * <p>這只是在 PostgreSQL 上對齊前置 boundary，不是正式 runtime。</p>
     */
    private static void seedBaseRows(DataSource dataSource) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            boolean originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                insertUser(connection, "pg-user-p14-t07", "pg-p14-t07@example.com");
                insertAccount(connection, "pg-acct-debit", "pg-user-p14-t07", "SPOT");
                insertAccount(connection, "pg-acct-credit", "pg-user-p14-t07", "MARGIN");
                insertAsset(connection, "USDT", "Tether USD");
                insertAccountAsset(connection, "pg-acct-debit", "USDT");
                insertAccountAsset(connection, "pg-acct-credit", "USDT");
                connection.commit();
            } finally {
                connection.setAutoCommit(originalAutoCommit);
            }
        }
    }

    /**
     * 插入 PostgreSQL verification 使用者。
     *
     * <p>這個方法只做 seed，不是正式 runtime。</p>
     */
    private static void insertUser(Connection connection, String userId, String email) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO users (user_id, email, display_name, status) VALUES (?, ?, ?, ?)")) {
            statement.setString(1, userId);
            statement.setString(2, email);
            statement.setString(3, "Phase 14 PG Test User");
            statement.setString(4, "ACTIVE");
            statement.executeUpdate();
        }
    }

    /**
     * 插入 PostgreSQL verification 帳戶。
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
     * 插入 PostgreSQL verification 資產。
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
     * 插入 PostgreSQL verification account_assets 關聯。
     *
     * <p>ledger_entries 必須透過 account_id + asset_symbol 對齊 account_assets。</p>
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
     * <p>這個方法只用在 verification，避免把通用查詢工具帶進正式 runtime。</p>
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
     * <p>這裡只驗證欄位映射，不把 adapter 升級成正式 runtime。</p>
     */
    private static void assertJournalRow(Connection connection, long ledgerJournalId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT business_reference_type, business_reference_id, request_id, journal_note, posted_at "
                        + "FROM ledger_journals WHERE ledger_journal_id = ?")) {
            statement.setLong(1, ledgerJournalId);

            try (ResultSet resultSet = statement.executeQuery()) {
                assertTrue(resultSet.next(), "ledger_journals row should exist");
                assertEquals("ORDER", resultSet.getString("business_reference_type"));
                assertEquals("pg-order-p14-t07-001", resultSet.getString("business_reference_id"));
                assertEquals(null, resultSet.getString("request_id"));
                assertEquals(null, resultSet.getString("journal_note"));
                OffsetDateTime postedAt = resultSet.getObject("posted_at", OffsetDateTime.class);
                assertTrue(postedAt != null, "posted_at should exist");
                assertEquals(Instant.parse("2026-07-08T04:00:00Z"), postedAt.toInstant());
            }
        }
    }

    /**
     * 驗證 entry row 只 append 到 ledger_entries。
     *
     * <p>這個方法只檢查 PostgreSQL append 結果，不處理任何 balance mutation。</p>
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
            }
        }
    }
}
