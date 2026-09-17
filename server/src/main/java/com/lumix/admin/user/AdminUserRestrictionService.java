package com.lumix.admin.user;

import com.lumix.admin.superadmin.SuperAdminAccessService;
import com.lumix.user.auth.domain.AuthenticatedUser;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 最高管理員對使用者登入與提幣的受治理限制。
 *
 * <p>HUMAN_REVIEW_REQUIRED：限制直接影響帳戶可用性與資產外流，所有有效狀態變更都會在同一交易內留下
 * immutable audit evidence；此服務不接觸錢包、私鑰、鏈上廣播或帳本餘額。</p>
 */
@Service
public class AdminUserRestrictionService {
    private final SuperAdminAccessService access;
    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;

    @Autowired
    public AdminUserRestrictionService(SuperAdminAccessService access, JdbcTemplate jdbcTemplate) {
        this(access, jdbcTemplate, Clock.systemUTC());
    }

    AdminUserRestrictionService(SuperAdminAccessService access, JdbcTemplate jdbcTemplate, Clock clock) {
        this.access = Objects.requireNonNull(access, "access must not be null");
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    /**
     * 使用既有 SUSPENDED 狀態凍結登入，並撤銷全部有效 session，避免舊 cookie 在限制生效後仍可繼續使用。
     */
    @Transactional
    public AdminUserRestrictionResult setLoginFrozen(AuthenticatedUser actor, String targetUserId, boolean frozen) {
        access.requireActiveSuperAdmin(Objects.requireNonNull(actor, "actor must not be null"));
        TargetState state = lockTarget(actor, targetUserId);
        boolean currentlyFrozen = "SUSPENDED".equals(state.status());
        if (currentlyFrozen == frozen) {
            return new AdminUserRestrictionResult(false, currentlyFrozen, state.withdrawalFrozenAt());
        }
        jdbcTemplate.update("UPDATE users SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE user_id = ?",
                frozen ? "SUSPENDED" : "ACTIVE", state.userId());
        if (frozen) {
            // 凍結登入必須同步撤銷既有 session，否則狀態只會影響下一次登入而非立即保護。
            jdbcTemplate.update("UPDATE user_sessions SET revoked_at = CURRENT_TIMESTAMP WHERE user_id = ? AND revoked_at IS NULL", state.userId());
        }
        writeAudit(actor.userId(), state.userId(), frozen ? "ADMIN_LOGIN_FROZEN" : "ADMIN_LOGIN_UNFROZEN",
                "status=" + state.status(), "status=" + (frozen ? "SUSPENDED" : "ACTIVE"));
        return new AdminUserRestrictionResult(true, frozen, state.withdrawalFrozenAt());
    }

    /**
     * 凍結僅限制使用者向他人或外部的出金；同一使用者現貨／合約間劃轉不應被此操作中斷。
     */
    @Transactional
    public AdminUserRestrictionResult setWithdrawalFrozen(AuthenticatedUser actor, String targetUserId, boolean frozen) {
        access.requireActiveSuperAdmin(Objects.requireNonNull(actor, "actor must not be null"));
        TargetState state = lockTarget(actor, targetUserId);
        boolean currentlyFrozen = state.withdrawalFrozenAt() != null;
        if (currentlyFrozen == frozen) {
            return new AdminUserRestrictionResult(false, "SUSPENDED".equals(state.status()), state.withdrawalFrozenAt());
        }
        Instant frozenAt = frozen ? Instant.now(clock) : null;
        jdbcTemplate.update("UPDATE users SET withdrawal_frozen_at = ?, updated_at = CURRENT_TIMESTAMP WHERE user_id = ?",
                frozenAt, state.userId());
        writeAudit(actor.userId(), state.userId(), frozen ? "ADMIN_WITHDRAWAL_FROZEN" : "ADMIN_WITHDRAWAL_UNFROZEN",
                "withdrawalFrozenAt=" + state.withdrawalFrozenAt(), "withdrawalFrozenAt=" + frozenAt);
        return new AdminUserRestrictionResult(true, "SUSPENDED".equals(state.status()), frozenAt);
    }

    private TargetState lockTarget(AuthenticatedUser actor, String targetUserId) {
        String target = Objects.requireNonNull(targetUserId, "targetUserId must not be null").trim();
        if (target.isEmpty() || target.length() > 64 || actor.userId().equals(target) || target.startsWith("system:")) {
            throw new IllegalArgumentException("target user is invalid");
        }
        List<TargetState> states = jdbcTemplate.query(
                "SELECT user_id, status, withdrawal_frozen_at FROM users WHERE user_id = ? FOR UPDATE",
                (row, number) -> new TargetState(row.getString("user_id"), row.getString("status"),
                        row.getTimestamp("withdrawal_frozen_at") == null ? null : row.getTimestamp("withdrawal_frozen_at").toInstant()), target);
        if (states.size() != 1 || "CLOSED".equals(states.getFirst().status())) {
            throw new IllegalArgumentException("target user is not eligible for restriction");
        }
        return states.getFirst();
    }

    private void writeAudit(String actorId, String targetUserId, String action, String before, String after) {
        jdbcTemplate.update(
                "INSERT INTO audit_logs (actor_type, actor_id, action_type, target_type, target_id, request_id, outcome, reason, before_state, after_state) "
                        + "VALUES ('ADMIN', ?, ?, 'USER', ?, ?, 'SUCCESS', 'governed user restriction', ?, ?)",
                actorId, action, targetUserId, "admin-user-restriction-" + UUID.randomUUID(), before, after);
    }

    private record TargetState(String userId, String status, Instant withdrawalFrozenAt) { }
}
