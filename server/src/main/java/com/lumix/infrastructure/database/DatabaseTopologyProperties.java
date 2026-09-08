package com.lumix.infrastructure.database;

import java.util.Locale;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 資料庫 topology 的部署設定。
 *
 * <p>single 只使用 primary；read-write-split 則要求獨立且已驗證同步狀態的 replica。這個設定
 * 不負責建立複寫，也不會把兩個無關 PostgreSQL instance 視為可讀寫分離架構。</p>
 */
@ConfigurationProperties("lumix.database")
public class DatabaseTopologyProperties {

    private Mode mode = Mode.SINGLE;
    private Connection primary = new Connection();
    private Connection replica = new Connection();

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode == null ? Mode.SINGLE : mode;
    }

    public Connection getPrimary() {
        return primary;
    }

    public void setPrimary(Connection primary) {
        this.primary = primary == null ? new Connection() : primary;
    }

    public Connection getReplica() {
        return replica;
    }

    public void setReplica(Connection replica) {
        this.replica = replica == null ? new Connection() : replica;
    }

    public enum Mode {
        SINGLE,
        READ_WRITE_SPLIT;

        /** 支援環境變數常用的 kebab case / snake case，避免部署端因字串格式默默退回 single。 */
        public static Mode from(String value) {
            return value == null ? SINGLE : Mode.valueOf(value.trim().replace('-', '_').toUpperCase(Locale.ROOT));
        }
    }

    /** 單一 JDBC endpoint 的最小連線資訊；密碼只能由環境變數或外部 secret 注入。 */
    public static class Connection {
        private String jdbcUrl;
        private String username;
        private String password;
        private int maximumPoolSize = 10;

        public String getJdbcUrl() {
            return jdbcUrl;
        }

        public void setJdbcUrl(String jdbcUrl) {
            this.jdbcUrl = jdbcUrl;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public int getMaximumPoolSize() {
            return maximumPoolSize;
        }

        public void setMaximumPoolSize(int maximumPoolSize) {
            this.maximumPoolSize = maximumPoolSize;
        }
    }
}
