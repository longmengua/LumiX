package com.lumix.database;

import org.flywaydb.core.Flyway;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class P12T03SchemaMigrationTest {

    @Test
    void migrationCreatesBalanceProjectionReadModelSchema() throws Exception {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:p12_t03;MODE=PostgreSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        dataSource.setPassword("");

        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .cleanDisabled(false)
                .load();

        flyway.clean();
        flyway.migrate();
        flyway.migrate();

        try (Connection connection = dataSource.getConnection()) {
            assertTableColumns(connection, "balance_projections",
                    "balance_projection_id",
                    "account_id",
                    "asset_symbol",
                    "total_amount",
                    "available_amount",
                    "locked_amount",
                    "projection_version",
                    "projected_at",
                    "reconciled_at");

            assertPrimaryKeyColumns(connection, "balance_projections", "balance_projection_id");
            assertUniqueIndexColumns(connection, "balance_projections", "account_id", "asset_symbol");
            assertForeignKeyColumns(connection, "fk_balance_projections_account_id", "balance_projections", "accounts", "account_id");
            assertForeignKeyColumns(connection, "fk_balance_projections_asset_symbol", "balance_projections", "assets", "asset_symbol");

            assertNumericType(connection, "balance_projections", "total_amount");
            assertNumericType(connection, "balance_projections", "available_amount");
            assertNumericType(connection, "balance_projections", "locked_amount");

            assertTableComment(connection, "BALANCE_PROJECTIONS",
                    "Account balance read model for query-side balance projection and reconciliation.");
            assertColumnComment(connection, "BALANCE_PROJECTIONS", "BALANCE_PROJECTION_ID",
                    "Surrogate identifier for the balance projection row.");
            assertColumnComment(connection, "BALANCE_PROJECTIONS", "ACCOUNT_ID",
                    "Reference to the owning account.");
            assertColumnComment(connection, "BALANCE_PROJECTIONS", "ASSET_SYMBOL",
                    "Reference to the asset whose balance is projected.");
            assertColumnComment(connection, "BALANCE_PROJECTIONS", "TOTAL_AMOUNT",
                    "Projected total balance for the account and asset.");
            assertColumnComment(connection, "BALANCE_PROJECTIONS", "AVAILABLE_AMOUNT",
                    "Projected available balance that is not locked.");
            assertColumnComment(connection, "BALANCE_PROJECTIONS", "LOCKED_AMOUNT",
                    "Projected locked balance held for orders or withdrawals.");
            assertColumnComment(connection, "BALANCE_PROJECTIONS", "PROJECTION_VERSION",
                    "Monotonic version of the projection row for rebuild and replay checks.");
            assertColumnComment(connection, "BALANCE_PROJECTIONS", "PROJECTED_AT",
                    "Timestamp when the balance projection was last refreshed.");
            assertColumnComment(connection, "BALANCE_PROJECTIONS", "RECONCILED_AT",
                    "Timestamp when the row was last reconciled against source data.");
        }
    }

    private static void assertTableColumns(Connection connection, String tableName, String... expectedColumns) throws SQLException {
        Set<String> actualColumns = new LinkedHashSet<>();
        DatabaseMetaData metaData = connection.getMetaData();

        try (ResultSet resultSet = metaData.getColumns(null, null, tableName, null)) {
            while (resultSet.next()) {
                actualColumns.add(resultSet.getString("COLUMN_NAME"));
            }
        }

        Set<String> expected = Set.of(expectedColumns);
        assertEquals(expected, actualColumns, "Unexpected columns for table " + tableName);
    }

    private static void assertPrimaryKeyColumns(Connection connection, String tableName, String... expectedColumns) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        Set<String> actualColumns = new LinkedHashSet<>();

        try (ResultSet resultSet = metaData.getPrimaryKeys(null, null, tableName)) {
            while (resultSet.next()) {
                actualColumns.add(resultSet.getString("COLUMN_NAME"));
            }
        }

        assertEquals(Set.of(expectedColumns), actualColumns, "Unexpected primary key columns for table " + tableName);
    }

    private static void assertUniqueIndexColumns(Connection connection, String tableName, String... expectedColumns) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        Set<String> expected = Set.of(expectedColumns);
        Map<String, Set<String>> uniqueIndexes = new LinkedHashMap<>();

        try (ResultSet resultSet = metaData.getIndexInfo(null, null, tableName, true, false)) {
            while (resultSet.next()) {
                String indexName = resultSet.getString("INDEX_NAME");
                String columnName = resultSet.getString("COLUMN_NAME");
                boolean nonUnique = resultSet.getBoolean("NON_UNIQUE");
                if (!nonUnique && indexName != null && columnName != null) {
                    uniqueIndexes.computeIfAbsent(indexName, ignored -> new LinkedHashSet<>()).add(columnName);
                }
            }
        }

        boolean found = uniqueIndexes.values().stream().anyMatch(columns -> columns.equals(expected));
        assertTrue(found, "Unique index not found for table " + tableName + " with columns " + expected);
    }

    private static void assertForeignKeyColumns(Connection connection, String fkName, String tableName, String referencedTableName, String... expectedColumns) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        Set<String> actualColumns = new LinkedHashSet<>();
        boolean found = false;

        try (ResultSet resultSet = metaData.getImportedKeys(null, null, tableName)) {
            while (resultSet.next()) {
                if (fkName.equalsIgnoreCase(resultSet.getString("FK_NAME"))
                        && referencedTableName.equalsIgnoreCase(resultSet.getString("PKTABLE_NAME"))) {
                    found = true;
                    actualColumns.add(resultSet.getString("FKCOLUMN_NAME"));
                }
            }
        }

        assertTrue(found, "Foreign key not found: " + fkName);
        assertEquals(Set.of(expectedColumns), actualColumns, "Unexpected foreign key columns for " + fkName);
    }

    private static void assertNumericType(Connection connection, String tableName, String columnName) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();

        try (ResultSet resultSet = metaData.getColumns(null, null, tableName, columnName)) {
            assertTrue(resultSet.next(), "Column not found: " + tableName + "." + columnName);
            String typeName = resultSet.getString("TYPE_NAME");
            assertTrue("NUMERIC".equalsIgnoreCase(typeName) || "DECIMAL".equalsIgnoreCase(typeName),
                    "Expected numeric column type for " + tableName + "." + columnName + " but was " + typeName);
        }
    }

    private static void assertTableComment(Connection connection, String tableName, String expectedComment) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT REMARKS FROM INFORMATION_SCHEMA.TABLES WHERE UPPER(TABLE_NAME) = UPPER(?)")) {
            statement.setString(1, tableName);
            try (ResultSet resultSet = statement.executeQuery()) {
            assertTrue(resultSet.next(), "Table not found in metadata: " + tableName);
            assertEquals(expectedComment, resultSet.getString("REMARKS"), "Unexpected table comment for " + tableName);
            }
        }
    }

    private static void assertColumnComment(Connection connection, String tableName, String columnName, String expectedComment) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT REMARKS FROM INFORMATION_SCHEMA.COLUMNS WHERE UPPER(TABLE_NAME) = UPPER(?) AND UPPER(COLUMN_NAME) = UPPER(?)")) {
            statement.setString(1, tableName);
            statement.setString(2, columnName);
            try (ResultSet resultSet = statement.executeQuery()) {
                assertTrue(resultSet.next(), "Column not found in metadata: " + tableName + "." + columnName);
                assertEquals(expectedComment, resultSet.getString("REMARKS"), "Unexpected column comment for " + tableName + "." + columnName);
            }
        }
    }
}
