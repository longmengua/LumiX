package com.lumix.admin.superadmin;

import com.lumix.user.auth.domain.AuthenticatedUser;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 最高管理員 PostgreSQL adapter。
 *
 * <p>啟用過程的 principal 與 password reset request 必須同一個 primary transaction 寫入，才能避免信件
 * 指向未建立或已被其他啟動程序取代的帳號。SQL 刻意不選取任何 credential、token 或 session 資料。</p>
 */
@Repository
@Profile("infrastructure")
class JdbcSuperAdminRepository implements SuperAdminRepository {

    private final JdbcTemplate jdbcTemplate;

    JdbcSuperAdminRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<SuperAdminPrincipal> lockSuperAdmin() {
        return jdbcTemplate.query(
            "SELECT u.user_id, u.email, u.display_name, a.status "
                + "FROM admin_principals a JOIN users u ON u.user_id = a.user_id "
                + "WHERE a.role = 'SUPER_ADMIN' FOR UPDATE",
            resultSet -> resultSet.next()
                ? Optional.of(new SuperAdminPrincipal(
                    new AuthenticatedUser(
                        resultSet.getString("user_id"), resultSet.getString("email"), resultSet.getString("display_name")
                    ),
                    SuperAdminPrincipal.State.valueOf(resultSet.getString("status"))
                ))
                : Optional.empty()
        );
    }

    @Override
    public Optional<AuthenticatedUser> findUserByEmail(String normalizedEmail) {
        return jdbcTemplate.query(
            "SELECT user_id, email, display_name FROM users WHERE email = ?",
            resultSet -> resultSet.next()
                ? Optional.of(new AuthenticatedUser(
                    resultSet.getString("user_id"), resultSet.getString("email"), resultSet.getString("display_name")
                ))
                : Optional.empty(),
            normalizedEmail
        );
    }

    @Override
    public void createBootstrapUser(AuthenticatedUser user, String passwordHash) {
        jdbcTemplate.update(
            "INSERT INTO users (user_id, email, display_name, status) VALUES (?, ?, ?, 'ACTIVE')",
            user.userId(), user.email(), user.displayName()
        );
        jdbcTemplate.update(
            "INSERT INTO user_credentials (user_id, password_hash, password_algorithm) VALUES (?, ?, 'BCRYPT')",
            user.userId(), passwordHash
        );
    }

    @Override
    public void createPendingSuperAdmin(String userId) {
        jdbcTemplate.update(
            "INSERT INTO admin_principals (user_id, role, status) VALUES (?, 'SUPER_ADMIN', 'PENDING_ACTIVATION')",
            userId
        );
    }

    @Override
    public void markActivationRequested(String userId) {
        jdbcTemplate.update(
            "UPDATE admin_principals SET activation_requested_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP "
                + "WHERE user_id = ? AND role = 'SUPER_ADMIN' AND status = 'PENDING_ACTIVATION'",
            userId
        );
    }

    @Override
    public boolean activatePendingSuperAdmin(String userId) {
        return jdbcTemplate.update(
            "UPDATE admin_principals SET status = 'ACTIVE', activated_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP "
                + "WHERE user_id = ? AND role = 'SUPER_ADMIN' AND status = 'PENDING_ACTIVATION'",
            userId
        ) == 1;
    }

    @Override
    public boolean isActiveSuperAdmin(String userId) {
        Boolean active = jdbcTemplate.queryForObject(
            "SELECT EXISTS (SELECT 1 FROM admin_principals a JOIN users u ON u.user_id = a.user_id "
                + "WHERE a.user_id = ? AND a.role = 'SUPER_ADMIN' AND a.status = 'ACTIVE' AND u.status = 'ACTIVE')",
            Boolean.class,
            userId
        );
        return Boolean.TRUE.equals(active);
    }

    @Override
    public void invalidateActivePasswordResets(String userId) {
        // 啟動重送啟用信時舊連結必須立即失效，避免遺失信件長時間保有平行的一次性入口。
        jdbcTemplate.update(
            "UPDATE password_reset_requests SET consumed_at = CURRENT_TIMESTAMP "
                + "WHERE user_id = ? AND consumed_at IS NULL AND expires_at > CURRENT_TIMESTAMP",
            userId
        );
    }

    @Override
    public void createPasswordReset(UUID requestId, String userId, String tokenDigest, Instant expiresAt) {
        jdbcTemplate.update(
            "INSERT INTO password_reset_requests (reset_request_id, user_id, token_digest, expires_at) VALUES (?, ?, ?, ?)",
            requestId, userId, tokenDigest, Timestamp.from(expiresAt)
        );
    }
}
