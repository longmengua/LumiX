package com.lumix.database;

import org.flywaydb.core.Flyway;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class P12T02SchemaMigrationTest {

    @Test
    void migrationCreatesIdentityUserAccountAssetFoundationSchema() throws Exception {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:p12_t02;MODE=PostgreSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1");
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
            assertTableColumns(connection, "users",
                    "user_id",
                    "email",
                    "display_name",
                    "status",
                    "created_at",
                    "updated_at");
            assertTableColumns(connection, "accounts",
                    "account_id",
                    "user_id",
                    "account_type",
                    "status",
                    "created_at",
                    "updated_at");
            assertTableColumns(connection, "assets",
                    "asset_symbol",
                    "display_name",
                    "precision_scale",
                    "status",
                    "created_at",
                    "updated_at");
            assertTableColumns(connection, "markets",
                    "market_symbol",
                    "base_asset_symbol",
                    "quote_asset_symbol",
                    "price_scale",
                    "quantity_scale",
                    "status",
                    "created_at",
                    "updated_at");
            assertTableColumns(connection, "account_assets",
                    "account_asset_id",
                    "account_id",
                    "asset_symbol",
                    "status",
                    "created_at",
                    "updated_at");

            assertPrimaryKeyColumns(connection, "users", "user_id");
            assertPrimaryKeyColumns(connection, "accounts", "account_id");
            assertPrimaryKeyColumns(connection, "assets", "asset_symbol");
            assertPrimaryKeyColumns(connection, "markets", "market_symbol");
            assertPrimaryKeyColumns(connection, "account_assets", "account_asset_id");

            assertUniqueIndexColumns(connection, "users", "email");
            assertUniqueIndexColumns(connection, "accounts", "user_id", "account_type");
            assertUniqueIndexColumns(connection, "markets", "base_asset_symbol", "quote_asset_symbol");
            assertUniqueIndexColumns(connection, "account_assets", "account_id", "asset_symbol");

            assertForeignKeyColumns(connection, "fk_accounts_user_id", "accounts", "users", "user_id");
            assertForeignKeyColumns(connection, "fk_markets_base_asset_symbol", "markets", "assets", "base_asset_symbol");
            assertForeignKeyColumns(connection, "fk_markets_quote_asset_symbol", "markets", "assets", "quote_asset_symbol");
            assertForeignKeyColumns(connection, "fk_account_assets_account_id", "account_assets", "accounts", "account_id");
            assertForeignKeyColumns(connection, "fk_account_assets_asset_symbol", "account_assets", "assets", "asset_symbol");
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
}
