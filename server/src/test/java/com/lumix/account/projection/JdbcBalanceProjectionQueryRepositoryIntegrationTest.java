package com.lumix.account.projection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import com.lumix.testing.JdbcDataSource;

/**
 * 驗證 query adapter 對齊真實 PostgreSQL schema，而不是只依 Mockito 假設欄位或 owner filtering 正確。
 */
class JdbcBalanceProjectionQueryRepositoryIntegrationTest {

    /** owner 的 projection 不可與另一位使用者同表資料混合，且 NUMERIC amount 必須完整保留。 */
    @Test
    void readsOnlyTheOwnersMaterializedProjectionFromPostgres() throws Exception {
        JdbcDataSource dataSource = createDataSource();
        migrate(dataSource);
        seed(dataSource);
        JdbcBalanceProjectionQueryRepository repository = new JdbcBalanceProjectionQueryRepository(new JdbcTemplate(dataSource));

        List<AccountBalanceProjection> projections = repository.findByOwnerUserId("user-owner");

        assertEquals(1, projections.size());
        AccountBalanceProjection projection = projections.getFirst();
        assertEquals("acct-owner-spot", projection.accountId().value());
        assertEquals("USDT", projection.assetSymbol().value());
        assertEquals(new BigDecimal("123456789012345678.123456789012345678"), projection.total().value());
        assertEquals(new BigDecimal("123456789012345678"), projection.available().value());
        assertEquals(new BigDecimal("0.123456789012345678"), projection.locked().value());
        assertEquals(BalanceProjectionFreshness.RECONCILED, projection.freshness());
        // 對方帳戶雖有同資產 projection，owner predicate 仍不得讓它進入結果。
        assertFalse(projections.stream().anyMatch(item -> item.accountId().value().equals("acct-other-spot")));
    }

    private static JdbcDataSource createDataSource() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:postgresql:test:asset_t01_projection_query");
        dataSource.setUser("ignored");
        dataSource.setPassword("ignored");
        return dataSource;
    }

    private static void migrate(JdbcDataSource dataSource) {
        Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration")
            .cleanDisabled(false)
            .load()
            .clean();
        Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration")
            .load()
            .migrate();
    }

    /** 測試資料只存在隔離的 lumix_test schema，用以證明 adapter 查的是真實 schema，不能作為產品 fallback。 */
    private static void seed(JdbcDataSource dataSource) throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            insertUser(connection, "user-owner", "owner@example.com");
            insertUser(connection, "user-other", "other@example.com");
            insertAsset(connection, "USDT", "Tether USD", 18);
            insertAccount(connection, "acct-owner-spot", "user-owner");
            insertAccount(connection, "acct-other-spot", "user-other");
            insertProjection(
                connection, "acct-owner-spot", "123456789012345678.123456789012345678",
                "123456789012345678.000000000000000000", "0.123456789012345678",
                Instant.parse("2026-09-15T00:00:00Z"), Instant.parse("2026-09-15T00:00:01Z")
            );
            insertProjection(
                connection, "acct-other-spot", "9.000000000000000000", "9.000000000000000000", "0.000000000000000000",
                Instant.parse("2026-09-15T00:00:00Z"), null
            );
        }
    }

    private static void insertUser(Connection connection, String userId, String email) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
            "INSERT INTO users (user_id, email, display_name, status) VALUES (?, ?, ?, 'ACTIVE')"
        )) {
            statement.setString(1, userId);
            statement.setString(2, email);
            statement.setString(3, userId);
            statement.executeUpdate();
        }
    }

    private static void insertAsset(Connection connection, String symbol, String displayName, int precisionScale) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
            "INSERT INTO assets (asset_symbol, display_name, precision_scale, status) VALUES (?, ?, ?, 'ACTIVE')"
        )) {
            statement.setString(1, symbol);
            statement.setString(2, displayName);
            statement.setInt(3, precisionScale);
            statement.executeUpdate();
        }
    }

    private static void insertAccount(Connection connection, String accountId, String userId) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
            "INSERT INTO accounts (account_id, user_id, account_type, status) VALUES (?, ?, 'SPOT', 'ACTIVE')"
        )) {
            statement.setString(1, accountId);
            statement.setString(2, userId);
            statement.executeUpdate();
        }
    }

    private static void insertProjection(
        Connection connection,
        String accountId,
        String total,
        String available,
        String locked,
        Instant projectedAt,
        Instant reconciledAt
    ) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
            "INSERT INTO balance_projections (account_id, asset_symbol, total_amount, available_amount, locked_amount, "
                + "projection_version, projected_at, reconciled_at) VALUES (?, 'USDT', ?, ?, ?, 12, ?, ?)"
        )) {
            statement.setString(1, accountId);
            statement.setBigDecimal(2, new BigDecimal(total));
            statement.setBigDecimal(3, new BigDecimal(available));
            statement.setBigDecimal(4, new BigDecimal(locked));
            statement.setObject(5, OffsetDateTime.ofInstant(projectedAt, ZoneOffset.UTC));
            statement.setObject(6, reconciledAt == null ? null : OffsetDateTime.ofInstant(reconciledAt, ZoneOffset.UTC));
            statement.executeUpdate();
        }
    }
}
