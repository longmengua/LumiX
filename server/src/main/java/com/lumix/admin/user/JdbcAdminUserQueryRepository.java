package com.lumix.admin.user;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
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
        + "u.fund_transfer_restricted_until, (SELECT MAX(s.created_at) FROM user_sessions s WHERE s.user_id = u.user_id) "
        + "AS last_login_at FROM users u ";

    private final JdbcTemplate jdbcTemplate;

    JdbcAdminUserQueryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<AdminUserSummary> find(String query, int limit) {
        String likeQuery = "%" + escapeLike(query) + "%";
        return jdbcTemplate.query(
            USER_PROJECTION
                + "WHERE u.user_id ILIKE ? ESCAPE '\\' OR u.email ILIKE ? ESCAPE '\\' "
                + "OR u.display_name ILIKE ? ESCAPE '\\' ORDER BY u.created_at DESC, u.user_id DESC LIMIT ?",
            (resultSet, rowNumber) -> mapUser(resultSet),
            likeQuery, likeQuery, likeQuery, limit
        );
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
            toInstant(resultSet.getTimestamp("fund_transfer_restricted_until"))
        );
    }

    private static Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }

    private static String escapeLike(String query) {
        return query.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
