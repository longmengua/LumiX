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

class P12T05SchemaMigrationTest {

    @Test
    void migrationCreatesOrderAndTradeExecutionSchema() throws Exception {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:p12_t05;MODE=PostgreSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1");
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
            // 這個測試只保護 schema 與 metadata，不觸碰任何 order 狀態轉換或成交 runtime。
            assertTableColumns(connection, "orders",
                    "order_id",
                    "user_id",
                    "account_id",
                    "market_symbol",
                    "side",
                    "order_type",
                    "time_in_force",
                    "status",
                    "price",
                    "quantity",
                    "filled_quantity",
                    "remaining_quantity",
                    "client_order_id",
                    "request_id",
                    "created_at",
                    "updated_at");
            assertTableColumns(connection, "trades",
                    "trade_id",
                    "market_symbol",
                    "maker_order_id",
                    "taker_order_id",
                    "maker_account_id",
                    "taker_account_id",
                    "price",
                    "quantity",
                    "quote_quantity",
                    "maker_fee_amount",
                    "taker_fee_amount",
                    "fee_asset_symbol",
                    "traded_at",
                    "created_at");

            assertPrimaryKeyColumns(connection, "orders", "order_id");
            assertPrimaryKeyColumns(connection, "trades", "trade_id");

            assertUniqueIndexColumns(connection, "orders", "request_id");
            assertUniqueIndexColumns(connection, "orders", "account_id", "client_order_id");

            assertForeignKeyColumns(connection, "fk_orders_user_id", "orders", "users", "user_id");
            assertForeignKeyColumns(connection, "fk_orders_account_id", "orders", "accounts", "account_id");
            assertForeignKeyColumns(connection, "fk_orders_market_symbol", "orders", "markets", "market_symbol");
            assertForeignKeyColumns(connection, "fk_trades_market_symbol", "trades", "markets", "market_symbol");
            assertForeignKeyColumns(connection, "fk_trades_maker_order_id", "trades", "orders", "maker_order_id");
            assertForeignKeyColumns(connection, "fk_trades_taker_order_id", "trades", "orders", "taker_order_id");
            assertForeignKeyColumns(connection, "fk_trades_maker_account_id", "trades", "accounts", "maker_account_id");
            assertForeignKeyColumns(connection, "fk_trades_taker_account_id", "trades", "accounts", "taker_account_id");
            assertForeignKeyColumns(connection, "fk_trades_fee_asset_symbol", "trades", "assets", "fee_asset_symbol");

            assertNumericType(connection, "orders", "price");
            assertNumericType(connection, "orders", "quantity");
            assertNumericType(connection, "orders", "filled_quantity");
            assertNumericType(connection, "orders", "remaining_quantity");
            assertNumericType(connection, "trades", "price");
            assertNumericType(connection, "trades", "quantity");
            assertNumericType(connection, "trades", "quote_quantity");
            assertNumericType(connection, "trades", "maker_fee_amount");
            assertNumericType(connection, "trades", "taker_fee_amount");

            assertCheckConstraintPresent(connection, "orders", "ck_orders_side");
            assertCheckConstraintPresent(connection, "orders", "ck_orders_order_type");
            assertCheckConstraintPresent(connection, "orders", "ck_orders_time_in_force");
            assertCheckConstraintPresent(connection, "orders", "ck_orders_status");
            assertCheckConstraintPresent(connection, "orders", "ck_orders_price_by_type");
            assertCheckConstraintPresent(connection, "orders", "ck_orders_quantity_positive");
            assertCheckConstraintPresent(connection, "orders", "ck_orders_filled_quantity_non_negative");
            assertCheckConstraintPresent(connection, "orders", "ck_orders_remaining_quantity_non_negative");
            assertCheckConstraintPresent(connection, "orders", "ck_orders_quantity_consistency");
            assertCheckConstraintPresent(connection, "trades", "ck_trades_distinct_orders");
            assertCheckConstraintPresent(connection, "trades", "ck_trades_price_positive");
            assertCheckConstraintPresent(connection, "trades", "ck_trades_quantity_positive");
            assertCheckConstraintPresent(connection, "trades", "ck_trades_quote_quantity_positive");
            assertCheckConstraintPresent(connection, "trades", "ck_trades_fee_amounts_non_negative");

            assertIndexColumns(connection, "orders", "idx_orders_user_id", "user_id");
            assertIndexColumns(connection, "orders", "idx_orders_account_id", "account_id");
            assertIndexColumns(connection, "orders", "idx_orders_market_status_created_at",
                    "market_symbol", "status", "created_at");
            assertIndexColumns(connection, "orders", "idx_orders_status_created_at", "status", "created_at");
            assertIndexColumns(connection, "trades", "idx_trades_market_traded_at", "market_symbol", "traded_at");
            assertIndexColumns(connection, "trades", "idx_trades_maker_order_id", "maker_order_id");
            assertIndexColumns(connection, "trades", "idx_trades_taker_order_id", "taker_order_id");
            assertIndexColumns(connection, "trades", "idx_trades_maker_account_id", "maker_account_id");
            assertIndexColumns(connection, "trades", "idx_trades_taker_account_id", "taker_account_id");

            // 高風險欄位的 comment 會直接影響後續維護者是否誤改 fee / quantity 語意。
            assertTableComment(connection, "ORDERS",
                    "訂單主檔。只保存 order lifecycle 所需的狀態與數值；狀態轉換、預留、成交與撤單流程都留給後續 runtime。");
            assertColumnComment(connection, "ORDERS", "ORDER_ID",
                    "訂單唯一識別碼。由應用層或上游流程產生，供查詢、審計與回溯使用。");
            assertColumnComment(connection, "ORDERS", "USER_ID",
                    "下單使用者。這是查詢與權限邊界的基本維度，不代表資金已被預留或扣款。");
            assertColumnComment(connection, "ORDERS", "ACCOUNT_ID",
                    "下單帳戶。order 以帳戶作為資金與交易邊界，避免把使用者層級的資料直接當成資金來源。");
            assertColumnComment(connection, "ORDERS", "MARKET_SYMBOL",
                    "交易對代號，例如 BTC-USDT。訂單只綁定單一 market，避免後續撮合時需要重新推導交易對。");
            assertColumnComment(connection, "ORDERS", "SIDE",
                    "訂單方向。BUY / SELL 只描述意圖，不代表已成交或已結算。");
            assertColumnComment(connection, "ORDERS", "ORDER_TYPE",
                    "訂單類型。LIMIT 與 MARKET 的欄位約束不同，schema 只負責約束格式，不負責撮合策略。");
            assertColumnComment(connection, "ORDERS", "TIME_IN_FORCE",
                    "訂單有效期。由應用層決定是否要填值；schema 只保留合法 enum 值。");
            assertColumnComment(connection, "ORDERS", "STATUS",
                    "訂單狀態。狀態值由應用層維護，schema 只限制合法值，不實作狀態轉換引擎。");
            assertColumnComment(connection, "ORDERS", "PRICE",
                    "委託價格，以 quote asset 數值表示。LIMIT 訂單必填，MARKET 訂單可為 NULL；價格階梯與撮合保護規則留到後續流程。");
            assertColumnComment(connection, "ORDERS", "QUANTITY",
                    "委託數量，以 base asset 數值表示。這是 order 的名義數量，不是可用餘額。");
            assertColumnComment(connection, "ORDERS", "FILLED_QUANTITY",
                    "已成交數量。後續撮合或結算可以累加，但必須和 remaining_quantity 保持一致。");
            assertColumnComment(connection, "ORDERS", "REMAINING_QUANTITY",
                    "剩餘未成交數量。此欄位保留查詢便利性，但仍要和 filled_quantity / quantity 一致。");
            assertColumnComment(connection, "ORDERS", "CLIENT_ORDER_ID",
                    "使用者自訂訂單代號。用來做客戶端重送防護與操作對帳，不應作為交易核心唯一主鍵。");
            assertColumnComment(connection, "ORDERS", "REQUEST_ID",
                    "請求識別碼。這是下單 idempotency 的核心欄位，避免重送造成重複下單。");
            assertColumnComment(connection, "ORDERS", "CREATED_AT",
                    "資料建立時間。由資料庫預設產生，供審計與歷史查詢使用。");
            assertColumnComment(connection, "ORDERS", "UPDATED_AT",
                    "資料最後更新時間。狀態變更時由應用層或後續 migration trigger 維護。");

            assertTableComment(connection, "TRADES",
                    "成交 / execution record。只記錄撮合後的結果與費用基礎欄位，不在這一階段綁定 settlement 或 ledger posting。");
            assertColumnComment(connection, "TRADES", "TRADE_ID",
                    "成交唯一識別碼。由撮合或 execution pipeline 產生，供後續稽核與查詢追蹤。");
            assertColumnComment(connection, "TRADES", "MARKET_SYMBOL",
                    "交易對代號。成交紀錄綁定單一 market，避免後續查詢需要再回推市場資訊。");
            assertColumnComment(connection, "TRADES", "MAKER_ORDER_ID",
                    "Maker 訂單。保留 maker / taker 關係，供撮合回放與費用追蹤使用。");
            assertColumnComment(connection, "TRADES", "TAKER_ORDER_ID",
                    "Taker 訂單。保留 maker / taker 關係，供撮合回放與費用追蹤使用。");
            assertColumnComment(connection, "TRADES", "MAKER_ACCOUNT_ID",
                    "Maker 所屬帳戶。供查詢與審計使用，不代表此階段已完成任何結算寫入。");
            assertColumnComment(connection, "TRADES", "TAKER_ACCOUNT_ID",
                    "Taker 所屬帳戶。供查詢與審計使用，不代表此階段已完成任何結算寫入。");
            assertColumnComment(connection, "TRADES", "PRICE",
                    "成交價格。使用 quote asset 單位記錄，必須保持 NUMERIC 精度。");
            assertColumnComment(connection, "TRADES", "QUANTITY",
                    "成交數量。使用 base asset 單位記錄，必須保持 NUMERIC 精度。");
            assertColumnComment(connection, "TRADES", "QUOTE_QUANTITY",
                    "成交金額。由 price × quantity 與精度規則推導後寫入，作為查詢與對帳便利欄位。");
            assertColumnComment(connection, "TRADES", "MAKER_FEE_AMOUNT",
                    "Maker 手續費。這是高風險欄位，費用精度與四捨五入政策必須在後續 review 後固定。");
            assertColumnComment(connection, "TRADES", "TAKER_FEE_AMOUNT",
                    "Taker 手續費。這是高風險欄位，費用精度與四捨五入政策必須在後續 review 後固定。");
            assertColumnComment(connection, "TRADES", "FEE_ASSET_SYMBOL",
                    "手續費幣別。費用可能與成交 base / quote 不同，這個欄位只記錄結果，不決定扣費路徑。");
            assertColumnComment(connection, "TRADES", "TRADED_AT",
                    "實際成交時間。用來表示 market execution 發生時間，與資料列寫入時間分開。");
            assertColumnComment(connection, "TRADES", "CREATED_AT",
                    "資料建立時間。與 traded_at 分開可避免查詢時把寫入延遲誤認為成交延遲。");
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
