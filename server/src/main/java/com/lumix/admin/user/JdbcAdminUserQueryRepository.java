package com.lumix.admin.user;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 使用者管理的 PostgreSQL 唯讀 adapter。
 *
 * <p>查詢刻意排除 password、cookie、token、完整 user-agent 與 IP；這些資料即使對最高管理員也不是
 * 使用者支援畫面所需資料。所有條件均採參數綁定，搜尋萬用字元會被跳脫。</p>
 */
@Repository
@Profile("infrastructure")
class JdbcAdminUserQueryRepository implements AdminUserQueryRepository {

    private static final String USER_PROJECTION = "SELECT u.user_id, u.email, u.display_name, u.status, u.created_at, "
        + "u.fund_transfer_restricted_until, u.withdrawal_frozen_at, "
        // 只聚合現有 schema 可證實的限制來源；新增充值等限制時必須在這裡擴充，避免前端自行猜測。
        + "(u.status = 'SUSPENDED' OR u.withdrawal_frozen_at IS NOT NULL OR u.fund_transfer_restricted_until > CURRENT_TIMESTAMP "
        + "OR EXISTS (SELECT 1 FROM accounts a WHERE a.user_id = u.user_id AND a.status = 'FROZEN')) "
        + "AS has_active_restriction, login_history.last_login_at FROM users u "
        + "LEFT JOIN LATERAL (SELECT s.created_at AS last_login_at FROM user_sessions s WHERE s.user_id = u.user_id "
        + "ORDER BY s.created_at DESC LIMIT 1) login_history ON TRUE ";

    private final JdbcTemplate jdbcTemplate;

    JdbcAdminUserQueryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<AdminUserSummary> find(AdminUserSearchCriteria criteria, int limit) {
        QueryParts queryParts = queryParts(criteria, true);
        List<Object> arguments = new ArrayList<>(queryParts.arguments());
        arguments.add(limit);
        return jdbcTemplate.query(
            USER_PROJECTION + queryParts.whereClause() + "ORDER BY u.created_at DESC, u.user_id DESC LIMIT ?",
            (resultSet, rowNumber) -> mapUser(resultSet),
            arguments.toArray()
        );
    }

    @Override
    public long count(AdminUserSearchCriteria criteria) {
        // cursor 只描述目前頁的起點，若帶入 COUNT 會把先前頁面排除，造成總筆數錯誤。
        QueryParts queryParts = queryParts(criteria, false);
        Long total = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM users u " + loginHistoryJoin() + queryParts.whereClause(),
            Long.class,
            queryParts.arguments().toArray()
        );
        return total == null ? 0 : total;
    }

    private static QueryParts queryParts(AdminUserSearchCriteria criteria, boolean includeCursor) {
        List<String> predicates = new ArrayList<>();
        List<Object> arguments = new ArrayList<>();

        if (!criteria.displayNamePrefix().isEmpty()) {
            // 僅在尾端附加 %：functional prefix index 才能作為候選資料集，而非掃描所有使用者。
            predicates.add("lower(u.display_name) LIKE ? ESCAPE '\\'");
            arguments.add(escapeLike(criteria.displayNamePrefix().toLowerCase(Locale.ROOT)) + "%");
        }
        if (criteria.createdFrom() != null) {
            predicates.add("u.created_at >= ?");
            arguments.add(Timestamp.from(criteria.createdFrom()));
        }
        if (criteria.createdBefore() != null) {
            predicates.add("u.created_at < ?");
            arguments.add(Timestamp.from(criteria.createdBefore()));
        }
        if (criteria.lastLoginFrom() != null) {
            // login_history 是每位使用者最新成功 session；不可改成 EXISTS，否則舊登入也會誤通過篩選。
            predicates.add("login_history.last_login_at >= ?");
            arguments.add(Timestamp.from(criteria.lastLoginFrom()));
        }
        if (criteria.lastLoginBefore() != null) {
            predicates.add("login_history.last_login_at < ?");
            arguments.add(Timestamp.from(criteria.lastLoginBefore()));
        }
        if (includeCursor && criteria.cursor() != null) {
            // 與 ORDER BY 完全相同的複合鍵，避免同一建立時間的資料在翻頁時遺漏或重複。
            predicates.add("(u.created_at, u.user_id) < (?, ?)");
            arguments.add(Timestamp.from(criteria.cursor().createdAt()));
            arguments.add(criteria.cursor().userId());
        }
        String whereClause = predicates.isEmpty() ? "" : "WHERE " + String.join(" AND ", predicates) + " ";
        return new QueryParts(whereClause, List.copyOf(arguments));
    }

    private static String loginHistoryJoin() {
        return "LEFT JOIN LATERAL (SELECT s.created_at AS last_login_at FROM user_sessions s WHERE s.user_id = u.user_id "
            + "ORDER BY s.created_at DESC LIMIT 1) login_history ON TRUE ";
    }

    @Override
    public Optional<AdminUserDetail> findById(String userId) {
        return jdbcTemplate.query(
            USER_PROJECTION + "WHERE u.user_id = ?",
            resultSet -> {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                AdminUserSummary user = mapUser(resultSet);
                return Optional.of(new AdminUserDetail(user, findActiveDevices(userId)));
            },
            userId
        );
    }

    private List<AdminUserDevice> findActiveDevices(String userId) {
        return jdbcTemplate.query(
            "SELECT device_platform, device_label, last_seen_at FROM user_login_devices "
                + "WHERE user_id = ? AND revoked_at IS NULL ORDER BY last_seen_at DESC, device_id DESC",
            (resultSet, rowNumber) -> new AdminUserDevice(
                resultSet.getString("device_platform"), resultSet.getString("device_label"),
                resultSet.getTimestamp("last_seen_at").toInstant()
            ),
            userId
        );
    }

    private static AdminUserSummary mapUser(ResultSet resultSet) throws SQLException {
        return new AdminUserSummary(
            resultSet.getString("user_id"),
            resultSet.getString("email"),
            resultSet.getString("display_name"),
            resultSet.getString("status"),
            resultSet.getTimestamp("created_at").toInstant(),
            toInstant(resultSet.getTimestamp("last_login_at")),
            toInstant(resultSet.getTimestamp("fund_transfer_restricted_until")),
            toInstant(resultSet.getTimestamp("withdrawal_frozen_at")),
            resultSet.getBoolean("has_active_restriction")
        );
    }

    private static Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }

    private static String escapeLike(String query) {
        return query.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    /** 查詢片段集中產生，確保資料列與 total 使用完全相同的 prefix 與時間篩選語意。 */
    private record QueryParts(String whereClause, List<Object> arguments) { }
}
