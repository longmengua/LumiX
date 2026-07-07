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

class P12T06SchemaMigrationTest {

    @Test
    void migrationCreatesReservationSchema() throws Exception {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:p12_t06;MODE=PostgreSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        dataSource.setPassword("");

        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .cleanDisabled(false)
                .load();

        flyway.clean();
        flyway.migrate();

        try (Connection connection = dataSource.getConnection()) {
            // 這個測試只保護 reservation schema 與 metadata，不觸碰任何 hold / release runtime。
            assertTableColumns(connection, "reservations",
                    "reservation_id",
                    "account_id",
                    "asset_symbol",
                    "business_reference_type",
                    "business_reference_id",
                    "reservation_type",
                    "status",
                    "original_amount",
                    "remaining_amount",
                    "consumed_amount",
                    "released_amount",
                    "request_id",
                    "expires_at",
                    "created_at",
                    "updated_at");

            assertPrimaryKeyColumns(connection, "reservations", "reservation_id");
            assertForeignKeyColumns(connection, "fk_reservations_account_asset", "reservations", "account_assets", "account_id", "asset_symbol");

            assertNumericType(connection, "reservations", "original_amount");
            assertNumericType(connection, "reservations", "remaining_amount");
            assertNumericType(connection, "reservations", "consumed_amount");
            assertNumericType(connection, "reservations", "released_amount");

            assertCheckConstraintPresent(connection, "reservations", "ck_reservations_business_reference_type");
            assertCheckConstraintPresent(connection, "reservations", "ck_reservations_reservation_type");
            assertCheckConstraintPresent(connection, "reservations", "ck_reservations_status");
            assertCheckConstraintPresent(connection, "reservations", "ck_reservations_original_amount_positive");
            assertCheckConstraintPresent(connection, "reservations", "ck_reservations_amounts_non_negative");
            assertCheckConstraintPresent(connection, "reservations", "ck_reservations_amounts_consistent");

            assertIndexColumns(connection, "reservations", "idx_reservations_account_id", "account_id");
            assertIndexColumns(connection, "reservations", "idx_reservations_asset_symbol", "asset_symbol");
            assertIndexColumns(connection, "reservations", "idx_reservations_account_asset_status", "account_id", "asset_symbol", "status");
            assertIndexColumns(connection, "reservations", "idx_reservations_business_reference", "business_reference_type", "business_reference_id");
            assertIndexColumns(connection, "reservations", "idx_reservations_request_id", "request_id");
            assertIndexColumns(connection, "reservations", "idx_reservations_expires_at", "expires_at");

            // 高風險欄位的 comment 必須提醒後續維護者：reservation 只是資料結構，不等於 ledger 或 balance 變動。
            assertTableComment(connection, "RESERVATIONS",
                    "預留主檔。這只是一份狀態資料結構，記錄某筆資產在特定業務目的下被保留、消耗或釋放的結果，不直接代表資金已扣款。");
            assertColumnComment(connection, "RESERVATIONS", "RESERVATION_ID",
                    "預留唯一識別碼。由應用層或上游流程產生，供查詢、對帳與回放使用。");
            assertColumnComment(connection, "RESERVATIONS", "ACCOUNT_ID",
                    "所屬帳戶。reservation 必須綁定 account，因為預留是以帳戶為邊界的資金控制資料。");
            assertColumnComment(connection, "RESERVATIONS", "ASSET_SYMBOL",
                    "所屬資產。與 account_id 一起約束 reservation 只可對既有 account_assets 建立。");
            assertColumnComment(connection, "RESERVATIONS", "BUSINESS_REFERENCE_TYPE",
                    "業務參照類型。用來標識 reservation 來源，例如 ORDER、WITHDRAWAL、SETTLEMENT 或 ADJUSTMENT。");
            assertColumnComment(connection, "RESERVATIONS", "BUSINESS_REFERENCE_ID",
                    "業務參照識別碼。指向對應業務物件，但不代表該物件已完成最終資金動作。");
            assertColumnComment(connection, "RESERVATIONS", "RESERVATION_TYPE",
                    "預留類型。描述這筆 reservation 的用途，例如下單、提款、結算或人工處理。");
            assertColumnComment(connection, "RESERVATIONS", "STATUS",
                    "預留狀態。這只描述資料上的生命週期，不等於 ledger 已經寫入或餘額已經結算。");
            assertColumnComment(connection, "RESERVATIONS", "ORIGINAL_AMOUNT",
                    "原始預留金額。使用 NUMERIC(38, 18) 保存，避免 binary floating point 誤差。");
            assertColumnComment(connection, "RESERVATIONS", "REMAINING_AMOUNT",
                    "剩餘可釋放或可再消耗的預留金額。這是查詢欄位，必須和其他金額欄位保持一致。");
            assertColumnComment(connection, "RESERVATIONS", "CONSUMED_AMOUNT",
                    "已消耗金額。這只表示 reservation 已被部分或完全用掉，不代表 ledger 已做最終 posting。");
            assertColumnComment(connection, "RESERVATIONS", "RELEASED_AMOUNT",
                    "已釋放金額。這只表示 reservation 已釋放回可用狀態，不代表任何 runtime 已完成扣款或退款。");
            assertColumnComment(connection, "RESERVATIONS", "REQUEST_ID",
                    "請求識別碼。只用於追蹤與降低重送風險，不可視為完整 idempotency 保證；完整 policy 留待後續 idempotency_keys。");
            assertColumnComment(connection, "RESERVATIONS", "EXPIRES_AT",
                    "到期時間。reservation 可能因業務規則到期釋放，但是否真正釋放仍由後續流程判斷。");
            assertColumnComment(connection, "RESERVATIONS", "CREATED_AT",
                    "資料建立時間。由資料庫預設產生，供審計與對帳使用。");
            assertColumnComment(connection, "RESERVATIONS", "UPDATED_AT",
                    "資料最後更新時間。狀態變更或對帳修正時由應用層維護。");
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

    private static void assertIndexColumns(Connection connection, String tableName, String indexName, String... expectedColumns) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        Set<String> actualColumns = new LinkedHashSet<>();
        boolean found = false;

        try (ResultSet resultSet = metaData.getIndexInfo(null, null, tableName, false, false)) {
            while (resultSet.next()) {
                if (indexName.equalsIgnoreCase(resultSet.getString("INDEX_NAME"))) {
                    found = true;
                    String columnName = resultSet.getString("COLUMN_NAME");
                    if (columnName != null) {
                        actualColumns.add(columnName);
                    }
                }
            }
        }

        assertTrue(found, "Index not found: " + tableName + "." + indexName);
        assertEquals(Set.of(expectedColumns), actualColumns, "Unexpected index columns for " + tableName + "." + indexName);
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
