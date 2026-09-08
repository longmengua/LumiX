package com.lumix.user.auth.persistence;

import com.lumix.user.auth.domain.AuthenticatedUser;
import com.lumix.user.auth.domain.LoginHistoryEntry;
import com.lumix.user.auth.domain.PasswordCredential;
import com.lumix.user.auth.domain.ResettableCredential;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * PostgreSQL 認證資料 adapter。
 *
 * <p>所有 SQL 使用參數綁定，且選取 credential/session 時刻意走 application 的 primary 交易，
 * 避免 read replica 延遲讓剛撤銷的 session 仍被接受。</p>
 */
@Repository
@Profile("infrastructure")
public class JdbcUserAuthenticationRepository implements UserAuthenticationRepository {

    private static final RowMapper<AuthenticatedUser> USER_ROW_MAPPER = (resultSet, rowNumber) ->
        new AuthenticatedUser(
            resultSet.getString("user_id"),
            resultSet.getString("email"),
            resultSet.getString("display_name")
        );

    private final JdbcTemplate jdbcTemplate;

    public JdbcUserAuthenticationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void createUser(AuthenticatedUser user, String passwordHash) {
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
    public Optional<PasswordCredential> findPasswordCredentialByEmail(String normalizedEmail) {
        return jdbcTemplate.query(
            "SELECT u.user_id, u.email, u.display_name, c.password_hash "
                + "FROM users u JOIN user_credentials c ON c.user_id = u.user_id "
                + "WHERE u.email = ? AND u.status = 'ACTIVE'",
            resultSet -> resultSet.next()
                ? Optional.of(new PasswordCredential(mapUser(resultSet), resultSet.getString("password_hash")))
                : Optional.empty(),
            normalizedEmail
        );
    }

    @Override
    public Optional<AuthenticatedUser> findActiveUserByEmail(String normalizedEmail) {
        return queryUser(
            "SELECT user_id, email, display_name FROM users WHERE email = ? AND status = 'ACTIVE'",
            normalizedEmail
        );
    }

    @Override
    public Optional<AuthenticatedUser> findActiveSession(UUID sessionId, String secretDigest) {
        return queryUser(
            "SELECT u.user_id, u.email, u.display_name "
                + "FROM user_sessions s JOIN users u ON u.user_id = s.user_id "
                + "WHERE s.session_id = ? AND s.token_digest = ? AND s.revoked_at IS NULL "
                + "AND s.expires_at > CURRENT_TIMESTAMP AND u.status = 'ACTIVE'",
            sessionId, secretDigest
        );
    }

    @Override
    public List<LoginHistoryEntry> findLoginHistory(String userId, int limit) {
        return jdbcTemplate.query(
            "SELECT created_at FROM user_sessions WHERE user_id = ? ORDER BY created_at DESC LIMIT ?",
            (resultSet, rowNumber) -> new LoginHistoryEntry(resultSet.getTimestamp("created_at").toInstant()),
            userId, limit
        );
    }

    @Override
    public void createSession(UUID sessionId, String userId, String secretDigest, Instant expiresAt) {
        jdbcTemplate.update(
            "INSERT INTO user_sessions (session_id, user_id, token_digest, expires_at) VALUES (?, ?, ?, ?)",
            // PostgreSQL JDBC 不支援直接綁定 Instant；轉為 Timestamp 可保留 UTC instant 語意。
            sessionId, userId, secretDigest, Timestamp.from(expiresAt)
        );
    }

    @Override
    public void revokeSession(UUID sessionId) {
        jdbcTemplate.update(
            "UPDATE user_sessions SET revoked_at = CURRENT_TIMESTAMP "
                + "WHERE session_id = ? AND revoked_at IS NULL",
            sessionId
        );
    }

    @Override
    public void revokeAllSessions(String userId) {
        jdbcTemplate.update(
            "UPDATE user_sessions SET revoked_at = CURRENT_TIMESTAMP "
                + "WHERE user_id = ? AND revoked_at IS NULL",
            userId
        );
    }

    @Override
    public void updatePasswordHash(String userId, String passwordHash) {
        jdbcTemplate.update(
            "UPDATE user_credentials SET password_hash = ?, password_changed_at = CURRENT_TIMESTAMP, "
                + "updated_at = CURRENT_TIMESTAMP WHERE user_id = ?",
            passwordHash, userId
        );
    }

    @Override
    public void createPasswordReset(UUID requestId, String userId, String secretDigest, Instant expiresAt) {
        jdbcTemplate.update(
            "INSERT INTO password_reset_requests (reset_request_id, user_id, token_digest, expires_at) "
                + "VALUES (?, ?, ?, ?)",
            requestId, userId, secretDigest, Timestamp.from(expiresAt)
        );
    }

    @Override
    public Optional<ResettableCredential> lockActivePasswordReset(String secretDigest) {
        return jdbcTemplate.query(
            "SELECT r.reset_request_id, u.user_id, u.email, u.display_name "
                + "FROM password_reset_requests r JOIN users u ON u.user_id = r.user_id "
                + "WHERE r.token_digest = ? AND r.consumed_at IS NULL AND r.expires_at > CURRENT_TIMESTAMP "
                + "AND u.status = 'ACTIVE' FOR UPDATE",
            resultSet -> resultSet.next()
                ? Optional.of(new ResettableCredential(
                    resultSet.getObject("reset_request_id", UUID.class), mapUser(resultSet)
                ))
                : Optional.empty(),
            secretDigest
        );
    }

    @Override
    public void consumePasswordReset(UUID requestId) {
        jdbcTemplate.update(
            "UPDATE password_reset_requests SET consumed_at = CURRENT_TIMESTAMP "
                + "WHERE reset_request_id = ? AND consumed_at IS NULL",
            requestId
        );
    }

    private Optional<AuthenticatedUser> queryUser(String sql, Object... arguments) {
        return jdbcTemplate.query(sql, resultSet -> resultSet.next()
            ? Optional.of(mapUser(resultSet))
            : Optional.empty(), arguments);
    }

    private static AuthenticatedUser mapUser(ResultSet resultSet) throws SQLException {
        return USER_ROW_MAPPER.mapRow(resultSet, 0);
    }
}
