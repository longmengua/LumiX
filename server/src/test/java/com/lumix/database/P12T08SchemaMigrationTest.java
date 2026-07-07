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

class P12T08SchemaMigrationTest {

    @Test
    void migrationCreatesOutboxAuditAndIdempotencySchema() throws Exception {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:p12_t08;MODE=PostgreSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1");
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
            // 這個測試先確認前序 schema 仍可完整載入，再驗證 V008 新增的 durable side-effect schema。
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

            assertPrimaryKeyColumns(connection, "idempotency_keys", "idempotency_key_id");
            assertPrimaryKeyColumns(connection, "outbox_events", "outbox_event_id");
            assertPrimaryKeyColumns(connection, "audit_logs", "audit_log_id");

            assertUniqueIndexColumns(connection, "idempotency_keys", "scope", "idempotency_key");

            assertCheckConstraintPresent(connection, "idempotency_keys", "ck_idempotency_keys_scope");
            assertCheckConstraintPresent(connection, "idempotency_keys", "ck_idempotency_keys_status");
            assertCheckConstraintPresent(connection, "idempotency_keys", "ck_idempotency_keys_response_code");
            assertCheckConstraintPresent(connection, "outbox_events", "ck_outbox_events_status");
            assertCheckConstraintPresent(connection, "outbox_events", "ck_outbox_events_retry_count");
            assertCheckConstraintPresent(connection, "audit_logs", "ck_audit_logs_actor_type");
            assertCheckConstraintPresent(connection, "audit_logs", "ck_audit_logs_outcome");

            assertIndexColumns(connection, "idempotency_keys", "idx_idempotency_keys_scope_status", "scope", "status");
            assertIndexColumns(connection, "idempotency_keys", "idx_idempotency_keys_request_id", "request_id");
            assertIndexColumns(connection, "idempotency_keys", "idx_idempotency_keys_expires_at", "expires_at");
            assertIndexColumns(connection, "idempotency_keys", "idx_idempotency_keys_resource_reference", "resource_type", "resource_id");
            assertIndexColumns(connection, "outbox_events", "idx_outbox_events_status_available_at", "status", "available_at");
            assertIndexColumns(connection, "outbox_events", "idx_outbox_events_aggregate_reference", "aggregate_type", "aggregate_id");
            assertIndexColumns(connection, "outbox_events", "idx_outbox_events_event_type", "event_type");
            assertIndexColumns(connection, "outbox_events", "idx_outbox_events_request_id", "request_id");
            assertIndexColumns(connection, "outbox_events", "idx_outbox_events_created_at", "created_at");
            assertIndexColumns(connection, "audit_logs", "idx_audit_logs_actor", "actor_type", "actor_id");
            assertIndexColumns(connection, "audit_logs", "idx_audit_logs_action_type", "action_type");
            assertIndexColumns(connection, "audit_logs", "idx_audit_logs_target", "target_type", "target_id");
            assertIndexColumns(connection, "audit_logs", "idx_audit_logs_request_id", "request_id");
            assertIndexColumns(connection, "audit_logs", "idx_audit_logs_created_at", "created_at");

            // 高風險欄位 comment 必須清楚說明：這些只是 durable storage，不是 runtime 邏輯本身。
            assertTableComment(connection, "IDEMPOTENCY_KEYS",
                    "Idempotency key 主檔。這不是完整的 runtime idempotency 保證，而是讓高風險操作能以 scope + key 做去重與對帳。");
            assertColumnComment(connection, "IDEMPOTENCY_KEYS", "SCOPE",
                    "Idempotency 範圍。用來區分 ORDER_CREATE、WITHDRAWAL_REQUEST、LEDGER_POSTING、DEPOSIT_CONFIRMATION 等高風險操作。");
            assertColumnComment(connection, "IDEMPOTENCY_KEYS", "IDEMPOTENCY_KEY",
                    "Idempotency key 值。與 scope 組合後才具有唯一意義。");
            assertColumnComment(connection, "IDEMPOTENCY_KEYS", "REQUEST_ID",
                    "請求識別碼。只用於追蹤與關聯，不可被誤解成單欄位完成完整 idempotency 保證。");
            assertColumnComment(connection, "IDEMPOTENCY_KEYS", "STATUS",
                    "Idempotency 狀態。只描述處理流程狀態，不代表 runtime 已完成副作用。");

            assertTableComment(connection, "OUTBOX_EVENTS",
                    "Outbox 事件主檔。這只保存 transactional outbox 所需的持久化事件，不會在 migration 內發送事件。");
            assertColumnComment(connection, "OUTBOX_EVENTS", "AGGREGATE_TYPE",
                    "聚合類型。用來指向事件來源，例如 ORDER、WITHDRAWAL、DEPOSIT、RESERVATION、LEDGER_JOURNAL。");
            assertColumnComment(connection, "OUTBOX_EVENTS", "AGGREGATE_ID",
                    "聚合識別碼。用來定位產生事件的業務物件。");
            assertColumnComment(connection, "OUTBOX_EVENTS", "EVENT_TYPE",
                    "事件類型。描述此次 outbox 事件的語意，例如 ORDER_CREATED 或 WITHDRAWAL_APPROVED。");
            assertColumnComment(connection, "OUTBOX_EVENTS", "PAYLOAD",
                    "事件 payload。以文字保存序列化內容，避免 JSON 型別在 H2 / PostgreSQL 之間產生相容性問題。");
            assertColumnComment(connection, "OUTBOX_EVENTS", "STATUS",
                    "事件狀態。只描述 outbox 的配送流程，不代表已被外部系統消費。");
            assertColumnComment(connection, "OUTBOX_EVENTS", "RETRY_COUNT",
                    "重試次數。用來支援失敗重送與 dead-letter 條件。");
            assertColumnComment(connection, "OUTBOX_EVENTS", "AVAILABLE_AT",
                    "下次可用時間。讓 publisher 可以延後重送，而不是立刻 busy loop。");

            assertTableComment(connection, "AUDIT_LOGS",
                    "稽核紀錄主檔。這只保存高風險操作的可追溯紀錄，不包含任何 runtime writer 或 admin workflow。");
            assertColumnComment(connection, "AUDIT_LOGS", "ACTOR_TYPE",
                    "行為者類型。描述誰觸發了這次高風險操作，例如 USER、ADMIN、SYSTEM、SERVICE。");
            assertColumnComment(connection, "AUDIT_LOGS", "ACTOR_ID",
                    "行為者識別碼。用來定位實際操作者或服務主體。");
            assertColumnComment(connection, "AUDIT_LOGS", "ACTION_TYPE",
                    "動作類型。描述被審計的行為，例如 ORDER_CREATE、WITHDRAWAL_REQUEST、LEDGER_POSTING、ADMIN_ACTION。");
            assertColumnComment(connection, "AUDIT_LOGS", "TARGET_TYPE",
                    "目標類型。描述被影響的資源類別，例如 ORDER、WITHDRAWAL、DEPOSIT、RESERVATION、LEDGER_JOURNAL。");
            assertColumnComment(connection, "AUDIT_LOGS", "TARGET_ID",
                    "目標識別碼。描述被影響的具體資源。");
            assertColumnComment(connection, "AUDIT_LOGS", "REQUEST_ID",
                    "請求識別碼。用來串起原始請求與稽核紀錄，不代表完整 idempotency 保證。");
            assertColumnComment(connection, "AUDIT_LOGS", "OUTCOME",
                    "操作結果。記錄這次高風險操作是成功、失敗還是被拒絕。");
            assertColumnComment(connection, "AUDIT_LOGS", "BEFORE_STATE",
                    "變更前狀態快照。以文字保存，供後續審計與事故分析。");
            assertColumnComment(connection, "AUDIT_LOGS", "AFTER_STATE",
                    "變更後狀態快照。以文字保存，供後續審計與事故分析。");
            assertColumnComment(connection, "AUDIT_LOGS", "CREATED_AT",
                    "資料建立時間。這是稽核紀錄的時間錨點，之後不應更新。");
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
