package com.lumix.trading.runtime;

import com.lumix.admin.superadmin.SuperAdminAccessService;
import com.lumix.user.auth.domain.AuthenticatedUser;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 資料庫權威交易總開關；每次新單與風險巡檢都讀取目前值，不使用程序環境快照。 */
@Service
@Profile("infrastructure")
public class TradingRuntimeSettingsService {
    private final JdbcTemplate jdbc;
    private final SuperAdminAccessService access;

    public TradingRuntimeSettingsService(JdbcTemplate jdbc, SuperAdminAccessService access) {
        this.jdbc = jdbc;
        this.access = access;
    }

    public boolean enabled(String kind) {
        return current(kind).enabled();
    }

    public Setting current(String kind) {
        String normalized = normalize(kind);
        return jdbc.query("SELECT trading_kind,enabled,version,updated_by,updated_at "
                        + "FROM trading_runtime_settings WHERE trading_kind=?",
                (row, index) -> setting(row.getString("trading_kind"), row.getBoolean("enabled"),
                        row.getLong("version"), row.getString("updated_by"),
                        row.getTimestamp("updated_at").toInstant()), normalized)
                .stream().findFirst().orElseThrow(() -> new IllegalStateException("trading runtime setting is missing"));
    }

    public List<Setting> all() {
        return jdbc.query("SELECT trading_kind,enabled,version,updated_by,updated_at "
                        + "FROM trading_runtime_settings ORDER BY trading_kind",
                (row, index) -> setting(row.getString("trading_kind"), row.getBoolean("enabled"),
                        row.getLong("version"), row.getString("updated_by"),
                        row.getTimestamp("updated_at").toInstant()));
    }

    @Transactional
    public Setting update(AuthenticatedUser actor, String kind, boolean enabled, long expectedVersion) {
        access.requireActiveSuperAdmin(Objects.requireNonNull(actor, "admin actor is required"));
        String normalized = normalize(kind);
        Setting before = jdbc.queryForObject("SELECT trading_kind,enabled,version,updated_by,updated_at "
                        + "FROM trading_runtime_settings WHERE trading_kind=? FOR UPDATE",
                (row, index) -> setting(row.getString("trading_kind"), row.getBoolean("enabled"),
                        row.getLong("version"), row.getString("updated_by"),
                        row.getTimestamp("updated_at").toInstant()), normalized);
        if (before == null || before.version() != expectedVersion)
            throw new IllegalStateException("trading runtime setting version changed");
        long nextVersion = expectedVersion + 1;
        jdbc.update("UPDATE trading_runtime_settings SET enabled=?,version=?,updated_by=?,"
                        + "updated_at=CURRENT_TIMESTAMP WHERE trading_kind=?",
                enabled, nextVersion, actor.userId(), normalized);
        jdbc.update("INSERT INTO audit_logs(actor_type,actor_id,action_type,target_type,target_id,"
                        + "request_id,outcome,before_state,after_state) VALUES "
                        + "('ADMIN',?,'TRADING_RUNTIME_SETTING_UPDATE','TRADING_SETTING',?,?,'SUCCESS',?,?)",
                actor.userId(), normalized, "trading-setting-" + UUID.randomUUID(),
                Boolean.toString(before.enabled()), Boolean.toString(enabled));
        return current(normalized);
    }

    private static Setting setting(String kind, boolean enabled, long version, String updatedBy, Instant updatedAt) {
        return new Setting(kind, enabled, version, enabled ? "ENABLED" : "DISABLED", enabled ? 1 : 0,
                updatedBy, updatedAt);
    }

    private static String normalize(String kind) {
        if (kind == null) throw new IllegalArgumentException("trading kind is required");
        String normalized = kind.trim().toUpperCase(Locale.ROOT);
        if (!List.of("SPOT", "FUTURES").contains(normalized))
            throw new IllegalArgumentException("invalid trading kind");
        return normalized;
    }

    public record Setting(String kind, boolean enabled, long version, String status, int statusCode,
                          String updatedBy, Instant updatedAt) { }
}
