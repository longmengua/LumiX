package com.lumix.user.auth.persistence;

import com.lumix.user.auth.domain.AuthenticatedUser;
import com.lumix.user.auth.domain.BoundLoginDevice;
import com.lumix.user.auth.domain.BoundDevicePlatform;
import com.lumix.user.auth.domain.LoginHistoryEntry;
import com.lumix.user.auth.domain.LoginRequestMetadata;
import com.lumix.user.auth.domain.LoginVerificationRequest;
import com.lumix.user.auth.domain.PendingRegistration;
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

    /** 儲存或取代同一 email 的未驗證註冊申請；原始 code 與密碼都不得傳進這個 boundary。 */
    void upsertRegistrationVerification(PendingRegistration registration);

    /** 驗證程序必須在同一個 primary transaction 鎖定申請，避免兩個 browser 同時完成同一註冊。 */
    Optional<PendingRegistration> lockActiveRegistrationVerification(UUID registrationId);

    /** 錯誤次數達上限會原子消耗申請，避免短碼被無限制暴力嘗試。 */
    void recordRegistrationVerificationFailure(UUID registrationId, int maxAttempts);

    /** 成功建立使用者後才可消耗申請；失敗 transaction 必須 rollback，避免有效申請被誤刪。 */
    void consumeRegistrationVerification(UUID registrationId);

    boolean userExistsByEmail(String normalizedEmail);

    Optional<PasswordCredential> findPasswordCredentialByEmail(String normalizedEmail);

    Optional<AuthenticatedUser> findActiveUserByEmail(String normalizedEmail);

    Optional<UserProfile> findActiveUserProfile(String userId);

    List<BoundLoginDevice> findActiveBoundLoginDevices(String userId);

    /** 先鎖住使用者列，讓同一帳戶的裝置槽位檢查與置換可以序列化。 */
    boolean lockActiveUserForBoundDeviceChange(String userId);

    boolean hasActiveBoundLoginDeviceForPlatform(String userId, BoundDevicePlatform platform);

    /** 只可延長、不可縮短裝置變更後的資金外流限制，避免併發換機提早解除保護。 */
    boolean extendFundTransferRestriction(String userId, Instant restrictedUntil);

    /** 僅供帳戶本人讀取倒數；真正提款與帳戶間轉帳入口仍須各自重新做 server-side gate。 */
    Optional<Instant> findFundTransferRestrictedUntil(String userId);

    Optional<AuthenticatedUser> findActiveSession(UUID sessionId, String secretDigest);

    Optional<TrustedLoginDevice> findActiveTrustedDevice(String userId, UUID deviceId, String secretDigest);

    List<LoginHistoryEntry> findLoginHistory(String userId, int limit);

    List<LoginHistoryEntry> findLoginHistoryOnOrBefore(String userId, Instant anchor, int limit);

    List<LoginHistoryEntry> findLoginHistoryBefore(String userId, Instant before, int limit);

    List<LoginHistoryEntry> findLoginHistoryAfter(String userId, Instant after, int limit);

    boolean hasLoginHistoryAfter(String userId, Instant occurredAt);

    void createTrustedDevice(UUID deviceId, String userId, String secretDigest, LoginRequestMetadata metadata);

    void touchTrustedDevice(UUID deviceId, LoginRequestMetadata metadata);

    /** 置換同一平台前必須撤銷舊 device 及其所有 active session，避免同一槽位短暫存在兩台裝置。 */
    void revokeActiveBoundDevicesForPlatform(String userId, BoundDevicePlatform platform);

    boolean revokeBoundLoginDevice(String userId, UUID deviceId);

    void revokeSessionsForDevice(String userId, UUID deviceId);

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
