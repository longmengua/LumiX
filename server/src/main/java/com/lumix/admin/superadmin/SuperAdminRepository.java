package com.lumix.admin.superadmin;

import com.lumix.user.auth.domain.AuthenticatedUser;
import java.time.Instant;
import java.util.Optional;

/** 最高管理員 bootstrap 與唯讀授權所需的最小持久化邊界。 */
interface SuperAdminRepository {

    Optional<SuperAdminPrincipal> lockSuperAdmin();

    Optional<AuthenticatedUser> findUserByEmail(String normalizedEmail);

    void createBootstrapUser(AuthenticatedUser user, String passwordHash);

    void createPendingSuperAdmin(String userId);

    void markActivationRequested(String userId);

    boolean activatePendingSuperAdmin(String userId);

    boolean isActiveSuperAdmin(String userId);

    void invalidateActivePasswordResets(String userId);

    void createPasswordReset(java.util.UUID requestId, String userId, String tokenDigest, Instant expiresAt);
}
