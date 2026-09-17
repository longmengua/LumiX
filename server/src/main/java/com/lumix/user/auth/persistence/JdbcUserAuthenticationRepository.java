package com.lumix.user.auth.persistence;

import com.lumix.user.auth.domain.AuthenticatedUser;
import com.lumix.user.auth.domain.BoundLoginDevice;
import com.lumix.user.auth.domain.BoundDevicePlatform;
import com.lumix.user.auth.domain.LoginHistoryEntry;
import com.lumix.user.auth.domain.LoginRequestMetadata;
import com.lumix.user.auth.domain.LoginVerificationRequest;
import com.lumix.user.auth.domain.LoginVerificationState;
import com.lumix.user.auth.domain.PendingRegistration;
import com.lumix.user.auth.domain.PasswordCredential;
import com.lumix.user.auth.domain.ResettableCredential;
import com.lumix.user.auth.domain.TrustedLoginDevice;
import com.lumix.user.auth.domain.UserProfile;
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
import org.springframework.jdbc.core.ResultSetExtractor;
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
        // 帳戶容器不含任何餘額；在同一個註冊 transaction 建立現貨與合約容器，避免後續資產 projection、
        // 風控或劃轉面對「已有使用者但缺帳戶類型」的半成品狀態。真正資金只能經後續 ledger runtime 進入。
        jdbcTemplate.update(
            "INSERT INTO accounts (account_id, user_id, account_type, status) VALUES "
                + "(?, ?, 'SPOT', 'ACTIVE'), (?, ?, 'FUTURES', 'ACTIVE')",
            accountId(user.userId(), "spot"), user.userId(),
            accountId(user.userId(), "futures"), user.userId()
        );
    }

    /** account id 以 immutable user id 與固定 type 組成，讓註冊 transaction 的資料關聯可追溯且不需隨機猜測。 */
    private static String accountId(String userId, String accountType) {
        return userId + ":" + accountType;
    }

    @Override
    public void upsertRegistrationVerification(PendingRegistration registration) {
        // 同一 email 的新寄送必須使舊信中的 code 立即無效，因此以 email unique key 原子覆寫申請。
        jdbcTemplate.update(
            "INSERT INTO registration_verification_requests (registration_id, user_id, email, display_name, password_hash, "
                + "numeric_code_digest, letter_code_digest, attempt_count, expires_at) VALUES (?, ?, ?, ?, ?, ?, ?, 0, ?) "
                + "ON CONFLICT (email) DO UPDATE SET registration_id = EXCLUDED.registration_id, user_id = EXCLUDED.user_id, "
                + "display_name = EXCLUDED.display_name, password_hash = EXCLUDED.password_hash, "
                + "numeric_code_digest = EXCLUDED.numeric_code_digest, letter_code_digest = EXCLUDED.letter_code_digest, "
                + "attempt_count = 0, expires_at = EXCLUDED.expires_at, consumed_at = NULL, updated_at = CURRENT_TIMESTAMP",
            registration.registrationId(), registration.user().userId(), registration.user().email(), registration.user().displayName(),
            registration.passwordHash(), registration.numericCodeDigest(), registration.letterCodeDigest(), Timestamp.from(registration.expiresAt())
        );
    }

    @Override
    public Optional<PendingRegistration> lockActiveRegistrationVerification(UUID registrationId) {
        return jdbcTemplate.query(
            "SELECT registration_id, user_id, email, display_name, password_hash, numeric_code_digest, letter_code_digest, "
                + "attempt_count, expires_at FROM registration_verification_requests "
                + "WHERE registration_id = ? AND consumed_at IS NULL AND expires_at > CURRENT_TIMESTAMP FOR UPDATE",
            resultSet -> resultSet.next()
                ? Optional.of(new PendingRegistration(
                    resultSet.getObject("registration_id", UUID.class),
                    new AuthenticatedUser(resultSet.getString("user_id"), resultSet.getString("email"), resultSet.getString("display_name")),
                    resultSet.getString("password_hash"), resultSet.getString("numeric_code_digest"),
                    resultSet.getString("letter_code_digest"), resultSet.getInt("attempt_count"),
                    resultSet.getTimestamp("expires_at").toInstant()
                ))
                : Optional.empty(),
            registrationId
        );
    }

    @Override
    public void recordRegistrationVerificationFailure(UUID registrationId, int maxAttempts) {
        jdbcTemplate.update(
            "UPDATE registration_verification_requests SET attempt_count = attempt_count + 1, "
                + "consumed_at = CASE WHEN attempt_count + 1 >= ? THEN CURRENT_TIMESTAMP ELSE consumed_at END, "
                + "updated_at = CURRENT_TIMESTAMP WHERE registration_id = ? AND consumed_at IS NULL",
            maxAttempts, registrationId
        );
    }

    @Override
    public void consumeRegistrationVerification(UUID registrationId) {
        jdbcTemplate.update(
            "UPDATE registration_verification_requests SET consumed_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP "
                + "WHERE registration_id = ? AND consumed_at IS NULL",
            registrationId
        );
    }

    @Override
    public boolean userExistsByEmail(String normalizedEmail) {
        Boolean exists = jdbcTemplate.queryForObject(
            "SELECT EXISTS (SELECT 1 FROM users WHERE email = ?)", Boolean.class, normalizedEmail
        );
        return Boolean.TRUE.equals(exists);
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
    public Optional<UserProfile> findActiveUserProfile(String userId) {
        return jdbcTemplate.query(
            "SELECT user_id, email, display_name, created_at FROM users WHERE user_id = ? AND status = 'ACTIVE'",
            resultSet -> resultSet.next()
                ? Optional.of(new UserProfile(
                    resultSet.getString("user_id"),
                    resultSet.getString("email"),
                    resultSet.getString("display_name"),
                    resultSet.getTimestamp("created_at").toInstant()
                ))
                : Optional.empty(),
            userId
        );
    }

    @Override
    public List<BoundLoginDevice> findActiveBoundLoginDevices(String userId) {
        return jdbcTemplate.query(
            "SELECT device_id, device_platform, device_label, last_ip_address, created_at, last_seen_at FROM user_login_devices "
                + "WHERE user_id = ? AND revoked_at IS NULL ORDER BY last_seen_at DESC, device_id DESC",
            (resultSet, rowNumber) -> new BoundLoginDevice(
                resultSet.getObject("device_id", UUID.class),
                BoundDevicePlatform.valueOf(resultSet.getString("device_platform")), resultSet.getString("device_label"),
                resultSet.getString("last_ip_address"), resultSet.getTimestamp("created_at").toInstant(),
                resultSet.getTimestamp("last_seen_at").toInstant()
            ),
            userId
        );
    }

    @Override
    public boolean lockActiveUserForBoundDeviceChange(String userId) {
        return jdbcTemplate.query(
            "SELECT user_id FROM users WHERE user_id = ? AND status = 'ACTIVE' FOR UPDATE",
            (ResultSetExtractor<Boolean>) resultSet -> resultSet.next(), userId
        );
    }

    @Override
    public boolean hasActiveBoundLoginDeviceForPlatform(String userId, BoundDevicePlatform platform) {
        Boolean exists = jdbcTemplate.queryForObject(
            "SELECT EXISTS (SELECT 1 FROM user_login_devices WHERE user_id = ? AND device_platform = ? "
                + "AND revoked_at IS NULL)",
            Boolean.class, userId, platform.name()
        );
        return Boolean.TRUE.equals(exists);
    }

    @Override
    public boolean extendFundTransferRestriction(String userId, Instant restrictedUntil) {
        return jdbcTemplate.update(
            "UPDATE users SET fund_transfer_restricted_until = GREATEST(COALESCE(fund_transfer_restricted_until, ?), ?), "
                + "updated_at = CURRENT_TIMESTAMP WHERE user_id = ? AND status = 'ACTIVE'",
            Timestamp.from(restrictedUntil), Timestamp.from(restrictedUntil), userId
        ) == 1;
    }

    @Override
    public Optional<Instant> findFundTransferRestrictedUntil(String userId) {
        return jdbcTemplate.query(
            "SELECT fund_transfer_restricted_until FROM users WHERE user_id = ? AND status = 'ACTIVE'",
            resultSet -> resultSet.next()
                ? Optional.ofNullable(resultSet.getTimestamp("fund_transfer_restricted_until"))
                    .map(Timestamp::toInstant)
                : Optional.empty(),
            userId
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
    public Optional<TrustedLoginDevice> findActiveTrustedDevice(String userId, UUID deviceId, String secretDigest) {
        return jdbcTemplate.query(
            "SELECT device_id, user_agent_digest FROM user_login_devices "
                + "WHERE user_id = ? AND device_id = ? AND token_digest = ? AND revoked_at IS NULL",
            resultSet -> resultSet.next()
                ? Optional.of(new TrustedLoginDevice(
                    resultSet.getObject("device_id", UUID.class), resultSet.getString("user_agent_digest")
                ))
                : Optional.empty(),
            userId, deviceId, secretDigest
        );
    }

    @Override
    public List<LoginHistoryEntry> findLoginHistory(String userId, int limit) {
        return queryLoginHistory(
            "SELECT created_at, ip_address, device_label FROM user_sessions WHERE user_id = ? "
                + "ORDER BY created_at DESC LIMIT ?", userId, limit
        );
    }

    @Override
    public List<LoginHistoryEntry> findLoginHistoryOnOrBefore(String userId, Instant anchor, int limit) {
        return queryLoginHistory(
            "SELECT created_at, ip_address, device_label FROM user_sessions WHERE user_id = ? AND created_at <= ? "
                + "ORDER BY created_at DESC LIMIT ?",
            userId, Timestamp.from(anchor), limit
        );
    }

    @Override
    public List<LoginHistoryEntry> findLoginHistoryBefore(String userId, Instant before, int limit) {
        return queryLoginHistory(
            "SELECT created_at, ip_address, device_label FROM user_sessions WHERE user_id = ? AND created_at < ? "
                + "ORDER BY created_at DESC LIMIT ?",
            userId, Timestamp.from(before), limit
        );
    }

    @Override
    public List<LoginHistoryEntry> findLoginHistoryAfter(String userId, Instant after, int limit) {
        return queryLoginHistory(
            "SELECT created_at, ip_address, device_label FROM user_sessions WHERE user_id = ? AND created_at > ? "
                + "ORDER BY created_at ASC LIMIT ?",
            userId, Timestamp.from(after), limit
        );
    }

    @Override
    public boolean hasLoginHistoryAfter(String userId, Instant occurredAt) {
        Boolean exists = jdbcTemplate.queryForObject(
            "SELECT EXISTS (SELECT 1 FROM user_sessions WHERE user_id = ? AND created_at > ?)",
            Boolean.class, userId, Timestamp.from(occurredAt)
        );
        return Boolean.TRUE.equals(exists);
    }

    @Override
    public void createTrustedDevice(UUID deviceId, String userId, String secretDigest, LoginRequestMetadata metadata) {
        jdbcTemplate.update(
            "INSERT INTO user_login_devices (device_id, user_id, token_digest, user_agent_digest, device_platform, device_label, "
                + "last_ip_address, last_seen_at) VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)",
            deviceId, userId, secretDigest, metadata.userAgentDigest(), metadata.devicePlatform().name(), metadata.deviceLabel(),
            metadata.ipAddress()
        );
    }

    @Override
    public void touchTrustedDevice(UUID deviceId, LoginRequestMetadata metadata) {
        jdbcTemplate.update(
            "UPDATE user_login_devices SET user_agent_digest = ?, device_label = ?, last_ip_address = ?, "
                + "last_seen_at = CURRENT_TIMESTAMP WHERE device_id = ? AND revoked_at IS NULL",
            metadata.userAgentDigest(), metadata.deviceLabel(), metadata.ipAddress(), deviceId
        );
    }

    @Override
    public void revokeActiveBoundDevicesForPlatform(String userId, BoundDevicePlatform platform) {
        // session 需在 device 尚為 active 時先撤銷，避免子查詢因前一步更新而遺漏舊 session。
        jdbcTemplate.update(
            "UPDATE user_sessions SET revoked_at = CURRENT_TIMESTAMP WHERE user_id = ? AND revoked_at IS NULL "
                + "AND device_id IN (SELECT device_id FROM user_login_devices WHERE user_id = ? "
                + "AND device_platform = ? AND revoked_at IS NULL)",
            userId, userId, platform.name()
        );
        jdbcTemplate.update(
            "UPDATE user_login_devices SET revoked_at = CURRENT_TIMESTAMP WHERE user_id = ? AND device_platform = ? "
                + "AND revoked_at IS NULL",
            userId, platform.name()
        );
    }

    @Override
    public boolean revokeBoundLoginDevice(String userId, UUID deviceId) {
        return jdbcTemplate.update(
            "UPDATE user_login_devices SET revoked_at = CURRENT_TIMESTAMP "
                + "WHERE user_id = ? AND device_id = ? AND revoked_at IS NULL",
            userId, deviceId
        ) == 1;
    }

    @Override
    public void revokeSessionsForDevice(String userId, UUID deviceId) {
        jdbcTemplate.update(
            "UPDATE user_sessions SET revoked_at = CURRENT_TIMESTAMP "
                + "WHERE user_id = ? AND device_id = ? AND revoked_at IS NULL",
            userId, deviceId
        );
    }

    @Override
    public void createLoginVerification(
        UUID verificationRequestId,
        String userId,
        String pendingTokenDigest,
        String approvalTokenDigest,
        UUID candidateDeviceId,
        String candidateDeviceTokenDigest,
        LoginRequestMetadata metadata,
        Instant expiresAt
    ) {
        jdbcTemplate.update(
            "INSERT INTO login_verification_requests (verification_request_id, user_id, pending_token_digest, "
                + "approval_token_digest, candidate_device_id, candidate_device_token_digest, user_agent_digest, "
                + "device_platform, device_label, ip_address, expires_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            verificationRequestId, userId, pendingTokenDigest, approvalTokenDigest, candidateDeviceId,
            candidateDeviceTokenDigest, metadata.userAgentDigest(), metadata.devicePlatform().name(), metadata.deviceLabel(), metadata.ipAddress(),
            Timestamp.from(expiresAt)
        );
    }

    @Override
    public Optional<LoginVerificationRequest> lockActiveLoginVerificationByApprovalToken(String approvalTokenDigest) {
        return lockLoginVerification(
            "SELECT r.verification_request_id, u.user_id, u.email, u.display_name, r.candidate_device_id, "
                + "r.candidate_device_token_digest, r.ip_address, r.device_label, r.user_agent_digest, r.device_platform, r.state, r.expires_at "
                + "FROM login_verification_requests r JOIN users u ON u.user_id = r.user_id "
                + "WHERE r.approval_token_digest = ? AND r.consumed_at IS NULL AND r.expires_at > CURRENT_TIMESTAMP "
                + "AND u.status = 'ACTIVE' FOR UPDATE",
            approvalTokenDigest
        );
    }

    @Override
    public Optional<LoginVerificationRequest> lockActiveLoginVerificationByPendingToken(
        UUID verificationRequestId,
        String pendingTokenDigest
    ) {
        return lockLoginVerification(
            "SELECT r.verification_request_id, u.user_id, u.email, u.display_name, r.candidate_device_id, "
                + "r.candidate_device_token_digest, r.ip_address, r.device_label, r.user_agent_digest, r.device_platform, r.state, r.expires_at "
                + "FROM login_verification_requests r JOIN users u ON u.user_id = r.user_id "
                + "WHERE r.verification_request_id = ? AND r.pending_token_digest = ? AND r.consumed_at IS NULL "
                + "AND r.expires_at > CURRENT_TIMESTAMP AND u.status = 'ACTIVE' FOR UPDATE",
            verificationRequestId, pendingTokenDigest
        );
    }

    @Override
    public boolean decideLoginVerification(UUID verificationRequestId, boolean approved) {
        return jdbcTemplate.update(
            "UPDATE login_verification_requests SET state = ?, decided_at = CURRENT_TIMESTAMP "
                + "WHERE verification_request_id = ? AND state = 'PENDING' AND consumed_at IS NULL "
                + "AND expires_at > CURRENT_TIMESTAMP",
            approved ? LoginVerificationState.APPROVED.name() : LoginVerificationState.REJECTED.name(), verificationRequestId
        ) == 1;
    }

    @Override
    public boolean consumeApprovedLoginVerification(UUID verificationRequestId) {
        return jdbcTemplate.update(
            "UPDATE login_verification_requests SET consumed_at = CURRENT_TIMESTAMP "
                + "WHERE verification_request_id = ? AND state = 'APPROVED' AND consumed_at IS NULL "
                + "AND expires_at > CURRENT_TIMESTAMP",
            verificationRequestId
        ) == 1;
    }

    @Override
    public void createSession(
        UUID sessionId,
        String userId,
        String secretDigest,
        Instant expiresAt,
        UUID deviceId,
        LoginRequestMetadata metadata
    ) {
        jdbcTemplate.update(
            "INSERT INTO user_sessions (session_id, user_id, token_digest, expires_at, device_id, ip_address, device_label) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)",
            // PostgreSQL JDBC 不支援直接綁定 Instant；轉為 Timestamp 可保留 UTC instant 語意。
            sessionId, userId, secretDigest, Timestamp.from(expiresAt), deviceId, metadata.ipAddress(), metadata.deviceLabel()
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
    public boolean updateDisplayName(String userId, String displayName) {
        return jdbcTemplate.update(
            "UPDATE users SET display_name = ?, updated_at = CURRENT_TIMESTAMP "
                + "WHERE user_id = ? AND status = 'ACTIVE'",
            displayName, userId
        ) == 1;
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

    private List<LoginHistoryEntry> queryLoginHistory(String sql, Object... arguments) {
        return jdbcTemplate.query(
            sql,
            (resultSet, rowNumber) -> new LoginHistoryEntry(
                resultSet.getTimestamp("created_at").toInstant(),
                resultSet.getString("ip_address"), resultSet.getString("device_label")
            ),
            arguments
        );
    }

    private Optional<LoginVerificationRequest> lockLoginVerification(String sql, Object... arguments) {
        return jdbcTemplate.query(sql, resultSet -> resultSet.next()
            ? Optional.of(new LoginVerificationRequest(
                resultSet.getObject("verification_request_id", UUID.class), mapUser(resultSet),
                resultSet.getObject("candidate_device_id", UUID.class), resultSet.getString("candidate_device_token_digest"),
                new LoginRequestMetadata(
                    resultSet.getString("ip_address"), resultSet.getString("device_label"), resultSet.getString("user_agent_digest"),
                    BoundDevicePlatform.valueOf(resultSet.getString("device_platform"))
                ),
                LoginVerificationState.valueOf(resultSet.getString("state")), resultSet.getTimestamp("expires_at").toInstant()
            ))
            : Optional.empty(), arguments);
    }

    private static AuthenticatedUser mapUser(ResultSet resultSet) throws SQLException {
        return USER_ROW_MAPPER.mapRow(resultSet, 0);
    }
}
