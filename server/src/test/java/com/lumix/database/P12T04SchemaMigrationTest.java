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

class P12T04SchemaMigrationTest {

    @Test
    void migrationCreatesImmutableLedgerSchema() throws Exception {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:p12_t04;MODE=PostgreSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1");
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
        flyway.migrate();

        try (Connection connection = dataSource.getConnection()) {
            assertTableColumns(connection, "ledger_journals",
                    "ledger_journal_id",
                    "business_reference_type",
                    "business_reference_id",
                    "request_id",
                    "journal_note",
                    "posted_at",
                    "created_at");
            assertTableColumns(connection, "ledger_entries",
                    "ledger_entry_id",
                    "ledger_journal_id",
                    "entry_sequence",
                    "account_id",
                    "asset_symbol",
                    "direction",
                    "amount",
                    "created_at");

            assertPrimaryKeyColumns(connection, "ledger_journals", "ledger_journal_id");
            assertPrimaryKeyColumns(connection, "ledger_entries", "ledger_entry_id");

            assertUniqueIndexColumns(connection, "ledger_entries", "ledger_journal_id", "entry_sequence");

            assertForeignKeyColumns(connection, "fk_ledger_entries_ledger_journal_id", "ledger_entries", "ledger_journals", "ledger_journal_id");
            assertForeignKeyColumns(connection, "fk_ledger_entries_account_id", "ledger_entries", "accounts", "account_id");
            assertForeignKeyColumns(connection, "fk_ledger_entries_asset_symbol", "ledger_entries", "assets", "asset_symbol");

            assertNumericType(connection, "ledger_entries", "amount");
            assertCheckConstraintPresent(connection, "ledger_journals", "ck_ledger_journals_reference_type");
            assertCheckConstraintPresent(connection, "ledger_entries", "ck_ledger_entries_sequence_positive");
            assertCheckConstraintPresent(connection, "ledger_entries", "ck_ledger_entries_direction");
            assertCheckConstraintPresent(connection, "ledger_entries", "ck_ledger_entries_amount_positive");

            assertTableComment(connection, "ledger_journals",
                    "Immutable journal header for ledger business events.");
            assertColumnComment(connection, "ledger_journals", "ledger_journal_id",
                    "Surrogate identifier for the journal header.");
            assertColumnComment(connection, "ledger_journals", "business_reference_type",
                    "Business event category such as deposit, withdrawal, order, trade, settlement, fee, or adjustment.");
            assertColumnComment(connection, "ledger_journals", "business_reference_id",
                    "Business-side reference identifier for the source event or aggregate.");
            assertColumnComment(connection, "ledger_journals", "request_id",
                    "Optional request identifier for idempotency or audit correlation.");
            assertColumnComment(connection, "ledger_journals", "journal_note",
                    "Optional human-readable note for audit and operations.");
            assertColumnComment(connection, "ledger_journals", "posted_at",
                    "Business timestamp when the journal was posted.");
            assertColumnComment(connection, "ledger_journals", "created_at",
                    "Database timestamp when the journal row was created.");

            assertTableComment(connection, "ledger_entries",
                    "Append-only double-entry lines for each immutable ledger journal.");
            assertColumnComment(connection, "ledger_entries", "ledger_entry_id",
                    "Surrogate identifier for the ledger line item.");
            assertColumnComment(connection, "ledger_entries", "ledger_journal_id",
                    "Reference to the immutable journal header that groups this entry.");
            assertColumnComment(connection, "ledger_entries", "entry_sequence",
                    "Journal-local sequence number for deterministic entry ordering.");
            assertColumnComment(connection, "ledger_entries", "account_id",
                    "Reference to the account affected by this entry.");
            assertColumnComment(connection, "ledger_entries", "asset_symbol",
                    "Reference to the asset affected by this entry.");
            assertColumnComment(connection, "ledger_entries", "direction",
                    "Debit or credit direction for double-entry accounting.");
            assertColumnComment(connection, "ledger_entries", "amount",
                    "Positive monetary amount for the entry.");
            assertColumnComment(connection, "ledger_entries", "created_at",
                    "Database timestamp when the entry row was created.");
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

        assertEquals(Set.of(expectedColumns), actualColumns, "Unexpected columns for table " + tableName);
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

    private static void assertCheckConstraintPresent(Connection connection, String tableName, String constraintName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT CONSTRAINT_NAME FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS WHERE UPPER(TABLE_NAME) = UPPER(?) AND UPPER(CONSTRAINT_NAME) = UPPER(?)")) {
            statement.setString(1, tableName);
            statement.setString(2, constraintName);
            try (ResultSet resultSet = statement.executeQuery()) {
                assertTrue(resultSet.next(), "Constraint not found: " + tableName + "." + constraintName);
            }
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
