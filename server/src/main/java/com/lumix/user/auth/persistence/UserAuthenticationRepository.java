package com.lumix.user.auth.persistence;

import com.lumix.user.auth.domain.AuthenticatedUser;
import com.lumix.user.auth.domain.LoginHistoryEntry;
import com.lumix.user.auth.domain.PasswordCredential;
import com.lumix.user.auth.domain.ResettableCredential;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** 認證 application service 唯一可依賴的持久化介面。 */
public interface UserAuthenticationRepository {

    void createUser(AuthenticatedUser user, String passwordHash);

    Optional<PasswordCredential> findPasswordCredentialByEmail(String normalizedEmail);

    Optional<AuthenticatedUser> findActiveUserByEmail(String normalizedEmail);

    Optional<AuthenticatedUser> findActiveSession(UUID sessionId, String secretDigest);

    List<LoginHistoryEntry> findLoginHistory(String userId, int limit);

    void createSession(UUID sessionId, String userId, String secretDigest, Instant expiresAt);

    void revokeSession(UUID sessionId);

    void revokeAllSessions(String userId);

    void updatePasswordHash(String userId, String passwordHash);

    void createPasswordReset(UUID requestId, String userId, String secretDigest, Instant expiresAt);

    Optional<ResettableCredential> lockActivePasswordReset(String secretDigest);

    void consumePasswordReset(UUID requestId);
}
