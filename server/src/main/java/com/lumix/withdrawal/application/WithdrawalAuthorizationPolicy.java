package com.lumix.withdrawal.application;

import com.lumix.api.error.ApiErrorCode;
import com.lumix.api.error.ApiException;
import com.lumix.withdrawal.config.WithdrawalPlatformProperties;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * 提款專用的 transaction authorization boundary。
 *
 * <p>目前尚未有 TOTP/passkey，故不可假裝完成 2FA；此政策至少要求有效且未超過受控年齡的 session。
 * 找不到 session 或時間資料時一律 fail closed，並保留未來 STEP_UP_REQUIRED 擴充位置。</p>
 */
@Service
public class WithdrawalAuthorizationPolicy {
    private final JdbcTemplate jdbc;
    private final WithdrawalPlatformProperties properties;

    public WithdrawalAuthorizationPolicy(JdbcTemplate jdbc, WithdrawalPlatformProperties properties) {
        this.jdbc = Objects.requireNonNull(jdbc);
        this.properties = Objects.requireNonNull(properties);
    }

    public void requireAuthorized(String userId, UUID sessionId) {
        if (sessionId == null || properties.getMaximumSessionAge() == null || properties.getMaximumSessionAge().isNegative()
            || properties.getMaximumSessionAge().isZero()) {
            throw new ApiException(ApiErrorCode.AUTHENTICATION_ERROR);
        }
        List<Map<String, Object>> rows = jdbc.queryForList(
            "SELECT created_at,expires_at,revoked_at FROM user_sessions WHERE session_id=? AND user_id=?", sessionId, userId
        );
        if (rows.size() != 1) throw new ApiException(ApiErrorCode.AUTHENTICATION_ERROR);
        Map<String, Object> session = rows.getFirst();
        Instant created = instant(session.get("created_at"));
        Instant expires = instant(session.get("expires_at"));
        if (created == null || expires == null || session.get("revoked_at") != null || !Instant.now().isBefore(expires)
            || created.plus(properties.getMaximumSessionAge()).isBefore(Instant.now())) {
            throw new ApiException(ApiErrorCode.AUTHENTICATION_ERROR);
        }
    }

    private static Instant instant(Object value) {
        return value instanceof java.sql.Timestamp timestamp ? timestamp.toInstant() : null;
    }
}
