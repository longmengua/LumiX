package com.lumix.trading.core.projection.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lumix.account.AccountId;
import com.lumix.account.AssetSymbol;
import com.lumix.ledger.domain.LedgerBusinessReferenceType;
import com.lumix.ledger.domain.LedgerDirection;
import com.lumix.ledger.domain.LedgerEntryDraft;
import com.lumix.ledger.domain.LedgerJournalDraft;
import com.lumix.ledger.persistence.DefaultLedgerAppendOnlyPersistenceMapper;
import com.lumix.ledger.persistence.adapter.LedgerAppendOnlyJdbcAdapter;
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
 * 驗證 balance projection rebuild gate 只把 ledger_entries 重建成 read model，不會變成 balance mutation runtime。
 */
class BalanceProjectionRebuildGateTest {

    /**
     * 確認 SPOT account 的 ledger entries 可以重建成 balance_projections，且 CREDIT / DEBIT 語意明確。
     *
     * 這個 case 必須存在，因為 projection rebuild 的第一步是把 ledger source of truth 安全地重建成可查詢的 read model。
     */
    @Test
    void rebuildsSpotProjectionRowsFromLedgerEntries() throws Exception {
        JdbcDataSource dataSource = createDataSource("jdbc:h2:mem:p15_t05_rebuild;MODE=PostgreSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1");
        migrate(dataSource);
        seedProjectionBoundaryRows(dataSource);
        appendLedgerHistory(dataSource);

        BalanceProjectionRebuildGate gate = new BalanceProjectionRebuildGate(dataSource::getConnection);

        try (Connection connection = dataSource.getConnection()) {
            assertEquals(3L, countRows(connection, "ledger_journals"));
            assertEquals(8L, countRows(connection, "ledger_entries"));
            assertEquals(0L, countRows(connection, "balance_projections"));
        }

        Instant projectedAt = Instant.parse("2026-07-11T10:00:00Z");
        BalanceProjectionRebuildResult result = gate.rebuild(projectedAt, 11L);

        assertEquals(2, result.projectedRowCount());
        assertEquals(11L, result.projectionVersion());
        assertEquals(projectedAt, result.projectedAt());

        try (Connection connection = dataSource.getConnection()) {
            assertEquals(3L, countRows(connection, "ledger_journals"));
            assertEquals(8L, countRows(connection, "ledger_entries"));
            assertEquals(2L, countRows(connection, "balance_projections"));

            assertProjectionRow(connection, "acct-spot-usdt", "USDT", new BigDecimal("75.000000"), 11L, projectedAt);
            assertProjectionRow(connection, "acct-spot-btc", "BTC", new BigDecimal("2.000000"), 11L, projectedAt);
        }
    }

    /**
     * 確認負數 projection total 會被拒絕，避免把不符合 schema 的 read model 寫回 balance_projections。
     *
     * 這個 case 必須存在，因為 CREDIT / DEBIT 的語意若讓 SPOT 帳戶彙總成負值，就代表目前的 projection contract 失效。
     */
    @Test
    void rebuildRejectsNegativeSpotProjectionTotals() throws Exception {
        JdbcDataSource dataSource = createDataSource("jdbc:h2:mem:p15_t05_negative;MODE=PostgreSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1");
        migrate(dataSource);
        seedProjectionBoundaryRows(dataSource);
        appendNegativeSpotLedgerHistory(dataSource);

        BalanceProjectionRebuildGate gate = new BalanceProjectionRebuildGate(dataSource::getConnection);

        assertThrows(IllegalStateException.class, () -> gate.rebuild(Instant.parse("2026-07-11T10:15:00Z"), 12L));

        try (Connection connection = dataSource.getConnection()) {
            assertEquals(1L, countRows(connection, "ledger_journals"));
            assertEquals(2L, countRows(connection, "ledger_entries"));
            assertEquals(0L, countRows(connection, "balance_projections"));
        }
    }

    /**
     * 確認 SPOT rebuild 不會刪掉非 SPOT 的 balance_projections rows。
     *
     * 這個 case 必須存在，因為 P15-T05 只能重建 SPOT read model，不能偷清整張 balance_projections。
     */
    @Test
    void rebuildKeepsNonSpotProjectionRows() throws Exception {
        JdbcDataSource dataSource = createDataSource("jdbc:h2:mem:p15_t05_non_spot_preserved;MODE=PostgreSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1");
        migrate(dataSource);
        seedProjectionBoundaryRows(dataSource);
        insertBalanceProjection(
                dataSource,
                "acct-clear-usdt",
                "USDT",
                new BigDecimal("999.000000"),
                new BigDecimal("999.000000"),
                BigDecimal.ZERO,
                77L,
                Instant.parse("2026-07-11T09:30:00Z"),
                Instant.parse("2026-07-11T09:30:00Z")
        );
        appendLedgerHistory(dataSource);

        BalanceProjectionRebuildGate gate = new BalanceProjectionRebuildGate(dataSource::getConnection);
        gate.rebuild(Instant.parse("2026-07-11T10:30:00Z"), 13L);

        try (Connection connection = dataSource.getConnection()) {
            assertEquals(3L, countRows(connection, "balance_projections"));
            assertProjectionRow(
                    connection,
                    "acct-clear-usdt",
                    "USDT",
                    new BigDecimal("999.000000"),
                    77L,
                    Instant.parse("2026-07-11T09:30:00Z")
            );
            assertProjectionRow(
                    connection,
                    "acct-spot-usdt",
                    "USDT",
                    new BigDecimal("75.000000"),
                    13L,
                    Instant.parse("2026-07-11T10:30:00Z")
            );
            assertEquals(3L, countRows(connection, "ledger_journals"));
            assertEquals(8L, countRows(connection, "ledger_entries"));
        }
    }

    /**
     * 建立 H2 DataSource。
     *
     * 這裡只供 gate 測試使用，不代表正式 runtime 已經完成。
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
     * 這樣測試才能直接對齊 Phase 12 與 Phase 15 的 schema contract。
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
     * 準備 SPOT projection 與 clearing account 的 boundary rows。
     *
     * <p>SPOT 帳戶會被 rebuild gate 投影到 balance_projections；clearing 帳戶保留在 ledger source of truth，
     * 但不屬於這次的 projection materialization 範圍。</p>
     */
    private static void seedProjectionBoundaryRows(JdbcDataSource dataSource) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            insertUser(connection, "user-p15-t05-spot-usdt", "p15-t05-spot-usdt@example.com");
            insertUser(connection, "user-p15-t05-spot-btc", "p15-t05-spot-btc@example.com");
            insertUser(connection, "user-p15-t05-clear-usdt", "p15-t05-clear-usdt@example.com");
            insertUser(connection, "user-p15-t05-clear-btc", "p15-t05-clear-btc@example.com");
            insertAccount(connection, "acct-spot-usdt", "user-p15-t05-spot-usdt", "SPOT");
            insertAccount(connection, "acct-spot-btc", "user-p15-t05-spot-btc", "SPOT");
            insertAccount(connection, "acct-clear-usdt", "user-p15-t05-clear-usdt", "MARGIN");
            insertAccount(connection, "acct-clear-btc", "user-p15-t05-clear-btc", "FUTURES");
            insertAsset(connection, "USDT", "Tether USD");
            insertAsset(connection, "BTC", "Bitcoin");
            insertAccountAsset(connection, "acct-spot-usdt", "USDT");
            insertAccountAsset(connection, "acct-spot-btc", "BTC");
            insertAccountAsset(connection, "acct-clear-usdt", "USDT");
            insertAccountAsset(connection, "acct-clear-btc", "BTC");
        }
    }

    /**
     * 以 append-only adapter 建立可重建的 ledger history。
     *
     * 這裡只 seed ledger，不把測試升級成正式 posting runtime。
     */
    private static void appendLedgerHistory(JdbcDataSource dataSource) {
        LedgerAppendOnlyJdbcAdapter adapter = new LedgerAppendOnlyJdbcAdapter(dataSource::getConnection);
        DefaultLedgerAppendOnlyPersistenceMapper mapper = new DefaultLedgerAppendOnlyPersistenceMapper();

        adapter.append(mapper.describeAppendOnlyMapping(
                new LedgerJournalDraft(
                        LedgerBusinessReferenceType.ORDER,
                        "order-p15-t05-001",
                        List.of(
                                new LedgerEntryDraft(new AccountId("acct-spot-usdt"), new AssetSymbol("USDT"), LedgerDirection.CREDIT,
                                        new BigDecimal("100.000000"), 1L),
                                new LedgerEntryDraft(new AccountId("acct-clear-usdt"), new AssetSymbol("USDT"), LedgerDirection.DEBIT,
                                        new BigDecimal("100.000000"), 2L),
                                new LedgerEntryDraft(new AccountId("acct-spot-btc"), new AssetSymbol("BTC"), LedgerDirection.CREDIT,
                                        new BigDecimal("2.000000"), 3L),
                                new LedgerEntryDraft(new AccountId("acct-clear-btc"), new AssetSymbol("BTC"), LedgerDirection.DEBIT,
                                        new BigDecimal("2.000000"), 4L)
                        )
                ),
                Instant.parse("2026-07-11T08:00:00Z")
        ));

        adapter.append(mapper.describeAppendOnlyMapping(
                new LedgerJournalDraft(
                        LedgerBusinessReferenceType.ORDER,
                        "order-p15-t05-002",
                        List.of(
                                new LedgerEntryDraft(new AccountId("acct-spot-usdt"), new AssetSymbol("USDT"), LedgerDirection.DEBIT,
                                        new BigDecimal("40.000000"), 1L),
                                new LedgerEntryDraft(new AccountId("acct-clear-usdt"), new AssetSymbol("USDT"), LedgerDirection.CREDIT,
                                        new BigDecimal("40.000000"), 2L)
                        )
                ),
                Instant.parse("2026-07-11T08:05:00Z")
        ));

        adapter.append(mapper.describeAppendOnlyMapping(
                new LedgerJournalDraft(
                        LedgerBusinessReferenceType.ORDER,
                        "order-p15-t05-003",
                        List.of(
                                new LedgerEntryDraft(new AccountId("acct-spot-usdt"), new AssetSymbol("USDT"), LedgerDirection.CREDIT,
                                        new BigDecimal("15.000000"), 1L),
                                new LedgerEntryDraft(new AccountId("acct-clear-usdt"), new AssetSymbol("USDT"), LedgerDirection.DEBIT,
                                        new BigDecimal("15.000000"), 2L)
                        )
                ),
                Instant.parse("2026-07-11T08:10:00Z")
        ));
    }

    /**
     * 建立會讓 spot projection 彙總成負值的 ledger history。
     *
     * 這個 case 用來驗證 rebuild gate 會先擋下與 schema 不相容的 projection 結果。
     */
    private static void appendNegativeSpotLedgerHistory(JdbcDataSource dataSource) {
        LedgerAppendOnlyJdbcAdapter adapter = new LedgerAppendOnlyJdbcAdapter(dataSource::getConnection);
        DefaultLedgerAppendOnlyPersistenceMapper mapper = new DefaultLedgerAppendOnlyPersistenceMapper();

        adapter.append(mapper.describeAppendOnlyMapping(
                new LedgerJournalDraft(
                        LedgerBusinessReferenceType.ORDER,
                        "order-p15-t05-negative-001",
                        List.of(
                                new LedgerEntryDraft(new AccountId("acct-spot-usdt"), new AssetSymbol("USDT"), LedgerDirection.DEBIT,
                                        new BigDecimal("25.000000"), 1L),
                                new LedgerEntryDraft(new AccountId("acct-clear-usdt"), new AssetSymbol("USDT"), LedgerDirection.CREDIT,
                                        new BigDecimal("25.000000"), 2L)
                        )
                ),
                Instant.parse("2026-07-11T08:20:00Z")
        ));
    }

    /**
     * 先寫入非 SPOT 的 balance projection row，方便驗證 SPOT rebuild 不會誤刪它。
     *
     * 這是純測試 seed，不是正式 runtime。
     */
    private static void insertBalanceProjection(
            JdbcDataSource dataSource,
            String accountId,
            String assetSymbol,
            BigDecimal totalAmount,
            BigDecimal availableAmount,
            BigDecimal lockedAmount,
            long projectionVersion,
            Instant projectedAt,
            Instant reconciledAt
    ) throws SQLException {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO balance_projections "
                                + "(account_id, asset_symbol, total_amount, available_amount, locked_amount, projection_version, projected_at, reconciled_at) "
                                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
            statement.setString(1, accountId);
            statement.setString(2, assetSymbol);
            statement.setBigDecimal(3, totalAmount);
            statement.setBigDecimal(4, availableAmount);
            statement.setBigDecimal(5, lockedAmount);
            statement.setLong(6, projectionVersion);
            statement.setObject(7, OffsetDateTime.ofInstant(projectedAt, java.time.ZoneOffset.UTC));
            statement.setObject(8, OffsetDateTime.ofInstant(reconciledAt, java.time.ZoneOffset.UTC));
            statement.executeUpdate();
        }
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
            statement.setString(3, "Phase 15 Projection Test User");
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
     * 插入測試用 account_assets 關聯。
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
     * 這裡只用在測試驗證，不代表正式 runtime 應該採用這種查詢模式。
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
     * 驗證 projection row 已正確 rebuild。
     *
     * 這裡只檢查 read model 的投影結果，不把它誤解成 source of truth。
     */
    private static void assertProjectionRow(
            Connection connection,
            String accountId,
            String assetSymbol,
            BigDecimal expectedTotal,
            long expectedProjectionVersion,
            Instant expectedProjectedAt
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT account_id, asset_symbol, total_amount, available_amount, locked_amount, projection_version, projected_at, reconciled_at "
                        + "FROM balance_projections WHERE account_id = ? AND asset_symbol = ?")) {
            statement.setString(1, accountId);
            statement.setString(2, assetSymbol);

            try (ResultSet resultSet = statement.executeQuery()) {
                assertTrue(resultSet.next(), "balance_projections row should exist for " + accountId + " / " + assetSymbol);
                assertEquals(accountId, resultSet.getString("account_id"));
                assertEquals(assetSymbol, resultSet.getString("asset_symbol"));
                assertEquals(0, expectedTotal.compareTo(resultSet.getBigDecimal("total_amount")));
                assertEquals(0, expectedTotal.compareTo(resultSet.getBigDecimal("available_amount")));
                assertEquals(0, BigDecimal.ZERO.compareTo(resultSet.getBigDecimal("locked_amount")));
                assertEquals(expectedProjectionVersion, resultSet.getLong("projection_version"));

                OffsetDateTime projectedAt = resultSet.getObject("projected_at", OffsetDateTime.class);
                OffsetDateTime reconciledAt = resultSet.getObject("reconciled_at", OffsetDateTime.class);
                assertNotNull(projectedAt);
                assertNotNull(reconciledAt);
                assertEquals(expectedProjectedAt, projectedAt.toInstant());
                assertEquals(expectedProjectedAt, reconciledAt.toInstant());
                assertFalse(resultSet.next());
            }
        }
    }
}
