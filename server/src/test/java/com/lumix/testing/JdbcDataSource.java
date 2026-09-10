package com.lumix.testing;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Objects;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.postgresql.ds.PGSimpleDataSource;

/**
 * 直接連線 PostgreSQL 的測試 DataSource。
 *
 * <p>每個舊有測試給一個 `jdbc:postgresql:test:{name}` 識別字，本類別會建立獨立 schema 並設為
 * current schema。因此 Flyway clean/migrate 永遠不會碰 Compose 的 public schema 或開發帳號資料。</p>
 */
public final class JdbcDataSource implements DataSource {

    private static final String TEST_URL_PREFIX = "jdbc:postgresql:test:";
    private final PGSimpleDataSource delegate = new PGSimpleDataSource();
    private String schema;

    /** 指定 test schema 識別字；正式 PostgreSQL endpoint 由 system property 或環境變數提供。 */
    public void setURL(String value) {
        if (value == null || !value.startsWith(TEST_URL_PREFIX)) {
            throw new IllegalArgumentException("PostgreSQL test data source requires jdbc:postgresql:test:{schema}");
        }
        String requestedSchema = value.substring(TEST_URL_PREFIX.length());
        if (!requestedSchema.matches("[a-z0-9_]{1,48}")) {
            throw new IllegalArgumentException("PostgreSQL test schema must be lowercase letters, digits, or underscores");
        }
        this.schema = "lumix_test_" + requestedSchema;
        delegate.setUrl(read("lumix.test.postgres.jdbc-url", "LUMIX_TEST_POSTGRES_JDBC_URL", "jdbc:postgresql://127.0.0.1:5432/lumix"));
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

    private static String read(String property, String environment, String fallback) {
        String systemValue = System.getProperty(property);
        if (systemValue != null && !systemValue.isBlank()) return systemValue;
        String environmentValue = System.getenv(environment);
        return environmentValue == null || environmentValue.isBlank() ? fallback : environmentValue;
    }
}
