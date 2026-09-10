package com.lumix.user.auth.persistence;

import com.lumix.user.auth.domain.AuthenticatedUser;
import com.lumix.user.auth.domain.LoginHistoryEntry;
import com.lumix.user.auth.domain.LoginRequestMetadata;
import com.lumix.user.auth.domain.LoginVerificationRequest;
import com.lumix.user.auth.domain.PasswordCredential;
import com.lumix.user.auth.domain.ResettableCredential;
import com.lumix.user.auth.domain.TrustedLoginDevice;
import com.lumix.user.auth.domain.UserProfile;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** 認證 application service 唯一可依賴的持久化介面。 */
public interface UserAuthenticationRepository {

    void createUser(AuthenticatedUser user, String passwordHash);

    Optional<PasswordCredential> findPasswordCredentialByEmail(String normalizedEmail);

    Optional<AuthenticatedUser> findActiveUserByEmail(String normalizedEmail);

    Optional<UserProfile> findActiveUserProfile(String userId);

    Optional<AuthenticatedUser> findActiveSession(UUID sessionId, String secretDigest);

    Optional<TrustedLoginDevice> findActiveTrustedDevice(String userId, UUID deviceId, String secretDigest);

    List<LoginHistoryEntry> findLoginHistory(String userId, int limit);

    List<LoginHistoryEntry> findLoginHistoryOnOrBefore(String userId, Instant anchor, int limit);

    List<LoginHistoryEntry> findLoginHistoryBefore(String userId, Instant before, int limit);

    List<LoginHistoryEntry> findLoginHistoryAfter(String userId, Instant after, int limit);

    boolean hasLoginHistoryAfter(String userId, Instant occurredAt);

    void createTrustedDevice(UUID deviceId, String userId, String secretDigest, LoginRequestMetadata metadata);

    void touchTrustedDevice(UUID deviceId, LoginRequestMetadata metadata);

    void createLoginVerification(
        UUID verificationRequestId,
        String userId,
        String pendingTokenDigest,
        String approvalTokenDigest,
        UUID candidateDeviceId,
        String candidateDeviceTokenDigest,
        LoginRequestMetadata metadata,
        Instant expiresAt
    );

    Optional<LoginVerificationRequest> lockActiveLoginVerificationByApprovalToken(String approvalTokenDigest);

    Optional<LoginVerificationRequest> lockActiveLoginVerificationByPendingToken(UUID verificationRequestId, String pendingTokenDigest);

    boolean decideLoginVerification(UUID verificationRequestId, boolean approved);

    boolean consumeApprovedLoginVerification(UUID verificationRequestId);

    void createSession(
        UUID sessionId,
        String userId,
        String secretDigest,
        Instant expiresAt,
        UUID deviceId,
        LoginRequestMetadata metadata
    );

    void revokeSession(UUID sessionId);

    void revokeAllSessions(String userId);

    void updatePasswordHash(String userId, String passwordHash);

    boolean updateDisplayName(String userId, String displayName);

    void createPasswordReset(UUID requestId, String userId, String secretDigest, Instant expiresAt);

    Optional<ResettableCredential> lockActivePasswordReset(String secretDigest);

    void consumePasswordReset(UUID requestId);
}
