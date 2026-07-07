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

class P12T07SchemaMigrationTest {

    @Test
    void migrationNormalizesWalletLifecycleLookupIndexes() throws Exception {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:p12_t07;MODE=PostgreSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1");
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
            // 這個測試先確認 V005 的 wallet lifecycle schema 仍然完整，再驗證 V007 只補強查詢索引。
            assertTableColumns(connection, "deposit_addresses",
                    "deposit_address_id",
                    "account_id",
                    "asset_symbol",
                    "chain_type",
                    "address",
                    "address_label",
                    "status",
                    "created_at",
                    "updated_at");
            assertTableColumns(connection, "chain_transactions",
                    "chain_transaction_id",
                    "chain_type",
                    "transaction_type",
                    "tx_hash",
                    "block_hash",
                    "block_height",
                    "from_address",
                    "to_address",
                    "amount",
                    "fee_amount",
                    "status",
                    "observed_at",
                    "confirmed_at",
                    "created_at",
                    "updated_at");
            assertTableColumns(connection, "deposits",
                    "deposit_id",
                    "account_id",
                    "asset_symbol",
                    "deposit_address_id",
                    "chain_transaction_id",
                    "chain_type",
                    "address",
                    "amount",
                    "confirmations",
                    "status",
                    "credited_at",
                    "created_at",
                    "updated_at");
            assertTableColumns(connection, "withdrawals",
                    "withdrawal_id",
                    "request_id",
                    "account_id",
                    "asset_symbol",
                    "chain_type",
                    "address",
                    "address_label",
                    "amount",
                    "fee_amount",
                    "chain_transaction_id",
                    "status",
                    "created_at",
                    "updated_at");

            assertPrimaryKeyColumns(connection, "deposit_addresses", "deposit_address_id");
            assertPrimaryKeyColumns(connection, "chain_transactions", "chain_transaction_id");
            assertPrimaryKeyColumns(connection, "deposits", "deposit_id");
            assertPrimaryKeyColumns(connection, "withdrawals", "withdrawal_id");

            assertUniqueIndexColumns(connection, "deposit_addresses", "chain_type", "address");
            assertUniqueIndexColumns(connection, "deposit_addresses", "account_id", "asset_symbol", "chain_type");
            assertUniqueIndexColumns(connection, "chain_transactions", "chain_type", "tx_hash");
            assertUniqueIndexColumns(connection, "deposits", "chain_transaction_id");
            assertUniqueIndexColumns(connection, "withdrawals", "request_id");
            assertUniqueIndexColumns(connection, "withdrawals", "chain_transaction_id");

            assertForeignKeyColumns(connection, "fk_deposit_addresses_account_asset", "deposit_addresses", "account_assets", "account_id", "asset_symbol");
            assertForeignKeyColumns(connection, "fk_deposits_account_asset", "deposits", "account_assets", "account_id", "asset_symbol");
            assertForeignKeyColumns(connection, "fk_deposits_deposit_address_id", "deposits", "deposit_addresses", "deposit_address_id");
            assertForeignKeyColumns(connection, "fk_deposits_chain_transaction_id", "deposits", "chain_transactions", "chain_transaction_id");
            assertForeignKeyColumns(connection, "fk_withdrawals_account_asset", "withdrawals", "account_assets", "account_id", "asset_symbol");
            assertForeignKeyColumns(connection, "fk_withdrawals_chain_transaction_id", "withdrawals", "chain_transactions", "chain_transaction_id");

            assertNumericType(connection, "chain_transactions", "amount");
            assertNumericType(connection, "chain_transactions", "fee_amount");
            assertNumericType(connection, "deposits", "amount");
            assertNumericType(connection, "withdrawals", "amount");
            assertNumericType(connection, "withdrawals", "fee_amount");

            assertCheckConstraintPresent(connection, "deposit_addresses", "ck_deposit_addresses_chain_type");
            assertCheckConstraintPresent(connection, "deposit_addresses", "ck_deposit_addresses_status");
            assertCheckConstraintPresent(connection, "chain_transactions", "ck_chain_transactions_chain_type");
            assertCheckConstraintPresent(connection, "chain_transactions", "ck_chain_transactions_transaction_type");
            assertCheckConstraintPresent(connection, "chain_transactions", "ck_chain_transactions_amount_positive");
            assertCheckConstraintPresent(connection, "chain_transactions", "ck_chain_transactions_fee_non_negative");
            assertCheckConstraintPresent(connection, "chain_transactions", "ck_chain_transactions_status");
            assertCheckConstraintPresent(connection, "deposits", "ck_deposits_chain_type");
            assertCheckConstraintPresent(connection, "deposits", "ck_deposits_amount_positive");
            assertCheckConstraintPresent(connection, "deposits", "ck_deposits_confirmations_non_negative");
            assertCheckConstraintPresent(connection, "deposits", "ck_deposits_status");
            assertCheckConstraintPresent(connection, "withdrawals", "ck_withdrawals_chain_type");
            assertCheckConstraintPresent(connection, "withdrawals", "ck_withdrawals_amount_positive");
            assertCheckConstraintPresent(connection, "withdrawals", "ck_withdrawals_fee_non_negative");
            assertCheckConstraintPresent(connection, "withdrawals", "ck_withdrawals_status");

            assertIndexColumns(connection, "deposit_addresses", "idx_deposit_addresses_account_id", "account_id");
            assertIndexColumns(connection, "deposit_addresses", "idx_deposit_addresses_asset_symbol", "asset_symbol");
            assertIndexColumns(connection, "deposit_addresses", "idx_deposit_addresses_status", "status");
            assertIndexColumns(connection, "deposit_addresses", "idx_deposit_addresses_address", "address");
            assertIndexColumns(connection, "chain_transactions", "idx_chain_transactions_chain_type_status", "chain_type", "status");
            assertIndexColumns(connection, "chain_transactions", "idx_chain_transactions_observed_at", "observed_at");
            assertIndexColumns(connection, "chain_transactions", "idx_chain_transactions_confirmed_at", "confirmed_at");
            assertIndexColumns(connection, "chain_transactions", "idx_chain_transactions_tx_hash", "tx_hash");
            assertIndexColumns(connection, "deposits", "idx_deposits_account_id", "account_id");
            assertIndexColumns(connection, "deposits", "idx_deposits_asset_symbol", "asset_symbol");
            assertIndexColumns(connection, "deposits", "idx_deposits_status", "status");
            assertIndexColumns(connection, "deposits", "idx_deposits_chain_transaction_id", "chain_transaction_id");
            assertIndexColumns(connection, "deposits", "idx_deposits_chain_type_address", "chain_type", "address");
            assertIndexColumns(connection, "withdrawals", "idx_withdrawals_account_id", "account_id");
            assertIndexColumns(connection, "withdrawals", "idx_withdrawals_asset_symbol", "asset_symbol");
            assertIndexColumns(connection, "withdrawals", "idx_withdrawals_status", "status");
            assertIndexColumns(connection, "withdrawals", "idx_withdrawals_chain_transaction_id", "chain_transaction_id");
            assertIndexColumns(connection, "withdrawals", "idx_withdrawals_chain_type_address", "chain_type", "address");

            // 高風險欄位的 comment 必須提醒後續維護者：這些只是查詢模型，不等於入帳、簽章或廣播完成。
            assertTableComment(connection, "DEPOSIT_ADDRESSES",
                    "使用者入金地址主檔。這只是一份可查詢的 address registry，不代表地址已被入帳，也不代表地址私鑰由本系統持有。");
            assertColumnComment(connection, "DEPOSIT_ADDRESSES", "ADDRESS",
                    "入金地址字串。只保存可供查詢的地址內容，不保存密鑰或簽章材料。");
            assertColumnComment(connection, "DEPOSIT_ADDRESSES", "STATUS",
                    "地址狀態。ACTIVE 表示可用；DISABLED 表示停用；ARCHIVED 表示歷史保留。");

            assertTableComment(connection, "CHAIN_TRANSACTIONS",
                    "鏈上交易主檔。這是 chain observer 的可查詢結果，不是 ledger source of truth，也不是入帳完成證明。");
            assertColumnComment(connection, "CHAIN_TRANSACTIONS", "TX_HASH",
                    "鏈上交易哈希。這是鏈層唯一識別碼，必須保留以便查詢與對帳。");
            assertColumnComment(connection, "CHAIN_TRANSACTIONS", "STATUS",
                    "鏈上交易狀態。只描述觀測與確認層級，不代表本地帳務已完成。");

            assertTableComment(connection, "DEPOSITS",
                    "充值紀錄。這只保存入金流程的查詢結果，不代表鏈上觀測一出現就已經完成 credit。");
            assertColumnComment(connection, "DEPOSITS", "ADDRESS",
                    "充值地址。為了對帳與稽核，保留當時使用的地址字串快照。");
            assertColumnComment(connection, "DEPOSITS", "STATUS",
                    "充值狀態。只描述入金流程狀態，不代表 ledger 已寫入。");

            assertTableComment(connection, "WITHDRAWALS",
                    "提現紀錄。這只保存提款流程的查詢結果，不代表已核准、已簽章或已廣播。");
            assertColumnComment(connection, "WITHDRAWALS", "REQUEST_ID",
                    "請求識別碼。這裡只做查詢與重送辨識的輔助欄位，不把它當成完整 idempotency 保證。");
            assertColumnComment(connection, "WITHDRAWALS", "ADDRESS",
                    "收款地址。只保存外部地址字串，不保存任何密鑰或簽章材料。");
            assertColumnComment(connection, "WITHDRAWALS", "STATUS",
                    "提現狀態。這些值只代表流程階段，不代表資金已移出。");
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
