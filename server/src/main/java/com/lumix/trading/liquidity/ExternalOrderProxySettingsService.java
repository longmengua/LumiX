package com.lumix.trading.liquidity;

import com.lumix.admin.superadmin.SuperAdminAccessService;
import com.lumix.user.auth.domain.AuthenticatedUser;
import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 每次操作都讀取資料庫開關，讓測試環境可即時停用 Demo provider，無須重啟服務。 */
@Service
public class ExternalOrderProxySettingsService {
    public static final String DEMO_KEY = "LUMIX_TRADING_EXTERNAL_PROXY_DEMO_ENABLED";
    private final JdbcTemplate jdbc;
    private final SuperAdminAccessService access;

    public ExternalOrderProxySettingsService(JdbcTemplate jdbc, SuperAdminAccessService access) {
        this.jdbc = jdbc;
        this.access = access;
    }

    /** 回傳目前資料庫版本；缺少 singleton row 時 fail closed。 */
    public Settings current() {
        return jdbc.query("SELECT enabled,version,updated_by,updated_at FROM external_order_proxy_settings "
                        + "WHERE setting_key=?", (row, index) -> new Settings(row.getBoolean("enabled"),
                        row.getLong("version"), row.getString("updated_by"),
                        row.getTimestamp("updated_at").toInstant()), DEMO_KEY)
                .stream().findFirst().orElseThrow(() -> new IllegalStateException("external proxy setting is missing"));
    }

    /** 關閉時以不存在語意拒絕 Demo，避免把 provider 能力暴露成可使用服務。 */
    public void requireDemoEnabled() {
        if (!current().demoEnabled()) throw new NoSuchElementException("demo external proxy is disabled");
    }

    /** 以版本檢查更新開關並追加稽核；設定與稽核必須在同一交易成功或回滾。 */
    @Transactional
    public Settings update(AuthenticatedUser actor, boolean enabled, long expectedVersion) {
        access.requireActiveSuperAdmin(Objects.requireNonNull(actor, "admin actor is required"));
        Settings before = jdbc.queryForObject("SELECT enabled,version,updated_by,updated_at "
                        + "FROM external_order_proxy_settings WHERE setting_key=? FOR UPDATE",
                (row, index) -> new Settings(row.getBoolean("enabled"), row.getLong("version"),
                        row.getString("updated_by"), row.getTimestamp("updated_at").toInstant()), DEMO_KEY);
        if (before == null || before.version() != expectedVersion)
            throw new IllegalStateException("external proxy setting version changed");
        long nextVersion = expectedVersion + 1;
        jdbc.update("UPDATE external_order_proxy_settings SET enabled=?,version=?,updated_by=?,"
                        + "updated_at=CURRENT_TIMESTAMP WHERE setting_key=?",
                enabled, nextVersion, actor.userId(), DEMO_KEY);
        String request = "proxy-setting-" + UUID.randomUUID();
        jdbc.update("INSERT INTO audit_logs(actor_type,actor_id,action_type,target_type,target_id,"
                        + "request_id,outcome,before_state,after_state) VALUES "
                        + "('ADMIN',?,'EXTERNAL_PROXY_SETTING_UPDATE','TRADING_SETTING',?,?,'SUCCESS',?,?)",
                actor.userId(), DEMO_KEY, request, Boolean.toString(before.demoEnabled()), Boolean.toString(enabled));
        return current();
    }

    public record Settings(boolean demoEnabled, long version, String updatedBy, Instant updatedAt) { }
}
