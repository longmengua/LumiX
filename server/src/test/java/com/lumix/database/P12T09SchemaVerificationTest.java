package com.lumix.database;

import org.flywaydb.core.Flyway;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class P12T09SchemaVerificationTest {

    @Test
    void allPhaseTwelveMigrationsApplyAndMoneyColumnsRemainExact() throws Exception {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:p12_t09;MODE=PostgreSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1");
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
            // 這個回歸測試確認 Phase 12 的 migration 可以從乾淨資料庫一路套用到最新版本。
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
            assertTableColumns(connection, "idempotency_keys",
                    "idempotency_key_id",
                    "scope",
                    "idempotency_key",
                    "request_id",
                    "status",
                    "resource_type",
                    "resource_id",
                    "response_code",
                    "response_summary",
                    "expires_at",
                    "created_at",
                    "updated_at");
            assertTableColumns(connection, "outbox_events",
                    "outbox_event_id",
                    "aggregate_type",
                    "aggregate_id",
                    "event_type",
                    "payload",
                    "status",
                    "retry_count",
                    "available_at",
                    "request_id",
                    "published_at",
                    "last_error",
                    "created_at",
                    "updated_at");
            assertTableColumns(connection, "audit_logs",
                    "audit_log_id",
                    "actor_type",
                    "actor_id",
                    "action_type",
                    "target_type",
                    "target_id",
                    "request_id",
                    "correlation_id",
                    "outcome",
                    "reason",
                    "before_state",
                    "after_state",
                    "created_at");

            // 這裡只驗證金額 / 價格 / 數量欄位都維持 exact numeric，不接受 FLOAT / DOUBLE / REAL。
            assertExactNumeric(connection, "balance_projections", "total_amount");
            assertExactNumeric(connection, "balance_projections", "available_amount");
            assertExactNumeric(connection, "balance_projections", "locked_amount");
            assertExactNumeric(connection, "ledger_entries", "amount");
            assertExactNumeric(connection, "orders", "price");
            assertExactNumeric(connection, "orders", "quantity");
            assertExactNumeric(connection, "orders", "filled_quantity");
            assertExactNumeric(connection, "orders", "remaining_quantity");
            assertExactNumeric(connection, "trades", "price");
            assertExactNumeric(connection, "trades", "quantity");
            assertExactNumeric(connection, "trades", "quote_quantity");
            assertExactNumeric(connection, "trades", "maker_fee_amount");
            assertExactNumeric(connection, "trades", "taker_fee_amount");
            assertExactNumeric(connection, "reservations", "original_amount");
            assertExactNumeric(connection, "reservations", "remaining_amount");
            assertExactNumeric(connection, "reservations", "consumed_amount");
            assertExactNumeric(connection, "reservations", "released_amount");
            assertExactNumeric(connection, "chain_transactions", "amount");
            assertExactNumeric(connection, "chain_transactions", "fee_amount");
            assertExactNumeric(connection, "deposits", "amount");
            assertExactNumeric(connection, "withdrawals", "amount");
            assertExactNumeric(connection, "withdrawals", "fee_amount");

            assertNoForbiddenFloatingTypes(connection);
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

    private static void assertExactNumeric(Connection connection, String tableName, String columnName) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();

        try (ResultSet resultSet = metaData.getColumns(null, null, tableName, columnName)) {
            assertTrue(resultSet.next(), "Column not found: " + tableName + "." + columnName);
            String typeName = resultSet.getString("TYPE_NAME");
            assertTrue(isExactNumeric(typeName),
                    "Expected exact numeric column type for " + tableName + "." + columnName + " but was " + typeName);
        }
    }

    private static boolean isExactNumeric(String typeName) {
        if (typeName == null) {
            return false;
        }
        String normalized = typeName.toUpperCase();
        return normalized.contains("DECIMAL") || normalized.contains("NUMERIC");
    }

    private static void assertNoForbiddenFloatingTypes(Connection connection) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        Set<String> forbiddenTypes = Set.of("FLOAT", "REAL", "DOUBLE", "DOUBLE PRECISION");

        try (ResultSet resultSet = metaData.getColumns(null, null, null, null)) {
            while (resultSet.next()) {
                String schemaName = resultSet.getString("TABLE_SCHEM");
                String typeName = resultSet.getString("TYPE_NAME");
                String tableName = resultSet.getString("TABLE_NAME");
                String columnName = resultSet.getString("COLUMN_NAME");
                if (!"PUBLIC".equalsIgnoreCase(schemaName)) {
                    continue;
                }
                if (typeName != null && forbiddenTypes.contains(typeName.toUpperCase())) {
                    throw new AssertionError("Forbidden floating type found at " + tableName + "." + columnName + " = " + typeName);
                }
            }
        }
    }
}
