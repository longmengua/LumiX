package com.lumix.testing;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.postgresql.ds.PGSimpleDataSource;

/**
 * 直接連線 PostgreSQL 的測試 DataSource。
 *
 * <p>每個舊有測試給一個 `jdbc:postgresql:test:{name}` 識別字，本類別會連至專用的 `lumix_test`
 * 資料庫，而非 Compose 的 `lumix`。每次 JVM 測試首次使用該資料庫時，會先刪除上一輪殘留的測試 schema，
 * 再建立本輪隔離 schema；因此 Flyway clean/migrate 永遠不會碰開發帳號資料。</p>
 */
public final class JdbcDataSource implements DataSource {

    private static final String TEST_URL_PREFIX = "jdbc:postgresql:test:";
    private static final String TEST_DATABASE_NAME = "lumix_test";
    private static final Set<String> RESET_DATABASES = new HashSet<>();
    private final PGSimpleDataSource delegate = new PGSimpleDataSource();
    private String schema;
    private String testDatabaseUrl;

    /** 指定 test schema 識別字；測試 endpoint 固定指向與應用資料分離的 lumix_test database。 */
    public void setURL(String value) {
        if (value == null || !value.startsWith(TEST_URL_PREFIX)) {
            throw new IllegalArgumentException("PostgreSQL test data source requires jdbc:postgresql:test:{schema}");
        }
        String requestedSchema = value.substring(TEST_URL_PREFIX.length());
        if (!requestedSchema.matches("[a-z0-9_]{1,48}")) {
            throw new IllegalArgumentException("PostgreSQL test schema must be lowercase letters, digits, or underscores");
        }
        this.schema = "lumix_test_" + requestedSchema;
        testDatabaseUrl = read("lumix.test.postgres.jdbc-url", "LUMIX_TEST_POSTGRES_JDBC_URL", "jdbc:postgresql://127.0.0.1:5432/lumix_test");
        delegate.setUrl(testDatabaseUrl);
        delegate.setUser(read("lumix.test.postgres.username", "LUMIX_TEST_POSTGRES_USERNAME", "lumix_app"));
        delegate.setPassword(read("lumix.test.postgres.password", "LUMIX_TEST_POSTGRES_PASSWORD", "replace-with-a-long-unique-password"));
        delegate.setCurrentSchema(schema);
    }

    /** 保留既有測試呼叫形狀，但認證一律由受控測試連線設定取得，不允許個別 case 覆寫。 */
    public void setUser(String ignored) { }

    /** 保留舊測試呼叫形狀，但認證一律由受控測試連線設定取得，不能被空密碼覆寫。 */
    public void setPassword(String ignored) { }

    @Override
    public Connection getConnection() throws SQLException {
        ensureConfigured();
        ensureTestDatabaseAndResetPreviousRun();
        ensureSchema();
        return delegate.getConnection();
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return getConnection();
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return delegate.getLogWriter();
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        delegate.setLogWriter(out);
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        delegate.setLoginTimeout(seconds);
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return delegate.getLoginTimeout();
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return delegate.getParentLogger();
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (iface.isInstance(this)) return iface.cast(this);
        return delegate.unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return iface.isInstance(this) || delegate.isWrapperFor(iface);
    }

    private void ensureConfigured() {
        Objects.requireNonNull(schema, "setURL must be called before opening a PostgreSQL test connection");
    }

    private void ensureSchema() throws SQLException {
        // schema 名稱由嚴格 regex 限制，才可安全放入 PostgreSQL identifier；不能以 bind parameter 傳入 DDL identifier。
        PGSimpleDataSource admin = new PGSimpleDataSource();
        admin.setUrl(delegate.getUrl());
        admin.setUser(delegate.getUser());
        admin.setPassword(delegate.getPassword());
        try (Connection connection = admin.getConnection(); var statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA IF NOT EXISTS " + schema);
        }
    }

    private void ensureTestDatabaseAndResetPreviousRun() throws SQLException {
        String testUrl = testDatabaseUrl;
        if (!testUrl.endsWith("/" + TEST_DATABASE_NAME)) {
            throw new SQLException("PostgreSQL test data source must target the dedicated " + TEST_DATABASE_NAME + " database");
        }
        synchronized (RESET_DATABASES) {
            if (RESET_DATABASES.contains(testUrl)) return;
            PGSimpleDataSource maintenance = new PGSimpleDataSource();
            maintenance.setUrl(testUrl.substring(0, testUrl.length() - TEST_DATABASE_NAME.length()) + "postgres");
            maintenance.setUser(delegate.getUser());
            maintenance.setPassword(delegate.getPassword());
            try (Connection connection = maintenance.getConnection(); Statement statement = connection.createStatement()) {
                try (var resultSet = statement.executeQuery("SELECT 1 FROM pg_database WHERE datname = '" + TEST_DATABASE_NAME + "'")) {
                    if (!resultSet.next()) statement.execute("CREATE DATABASE " + TEST_DATABASE_NAME);
                }
            }
            try (Connection connection = delegate.getConnection(); Statement statement = connection.createStatement();
                 var resultSet = statement.executeQuery(
                     "SELECT nspname FROM pg_namespace WHERE nspname LIKE 'lumix_test\\_%' ESCAPE '\\'"
                 )) {
                Set<String> previousSchemas = new HashSet<>();
                while (resultSet.next()) previousSchemas.add(resultSet.getString(1));
                for (String previousSchema : previousSchemas) {
                    // schema 名稱來自 PostgreSQL catalog；仍以 identifier quote 防止測試資料夾內含特殊字元時誤刪其他物件。
                    statement.execute("DROP SCHEMA " + quoteIdentifier(previousSchema) + " CASCADE");
                }
            }
            RESET_DATABASES.add(testUrl);
        }
    }

    private static String quoteIdentifier(String value) {
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private static String read(String property, String environment, String fallback) {
        String systemValue = System.getProperty(property);
        if (systemValue != null && !systemValue.isBlank()) return systemValue;
        String environmentValue = System.getenv(environment);
        return environmentValue == null || environmentValue.isBlank() ? fallback : environmentValue;
    }
}
