package com.lumix.user.auth.application;

import com.lumix.api.error.ApiErrorCode;
import com.lumix.api.error.ApiException;
import com.lumix.user.auth.config.UserAuthenticationProperties;
import com.lumix.user.auth.domain.AuthenticatedUser;
import com.lumix.user.auth.domain.BoundLoginDevice;
import com.lumix.user.auth.domain.DeviceSecret;
import com.lumix.user.auth.domain.LoginHistoryEntry;
import com.lumix.user.auth.domain.LoginHistoryPage;
import com.lumix.user.auth.domain.LoginRequestMetadata;
import com.lumix.user.auth.domain.LoginSecuritySettings;
import com.lumix.user.auth.domain.LoginVerificationRequest;
import com.lumix.user.auth.domain.LoginVerificationSecret;
import com.lumix.user.auth.domain.LoginVerificationState;
import com.lumix.user.auth.domain.PasswordCredential;
import com.lumix.user.auth.domain.PasswordResetSecret;
import com.lumix.user.auth.domain.PendingRegistration;
import com.lumix.user.auth.domain.PendingLoginVerificationSecret;
import com.lumix.user.auth.domain.RegistrationVerificationSecret;
import com.lumix.user.auth.domain.ResettableCredential;
import com.lumix.user.auth.domain.SessionSecret;
import com.lumix.user.auth.domain.TrustedLoginDevice;
import com.lumix.user.auth.domain.UserProfile;
import com.lumix.user.auth.persistence.UserAuthenticationRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.annotation.Profile;

/**
 * 使用者帳密、session 與密碼重設的應用服務。
 *
 * <p>所有方法都刻意使用 primary 寫入 transaction，即使是 session 驗證也不例外；安全決策不能
 * 受到 read replica 複寫延遲影響。密碼與 token 永遠只在記憶體內短暫存在。</p>
 */
@Service
@Profile("infrastructure")
public class UserAuthenticationService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int SECRET_BYTES = 32;
    private static final int DEFAULT_LOGIN_HISTORY_PAGE_SIZE = 10;
    private static final int MAX_LOGIN_HISTORY_PAGE_SIZE = 50;
    // 這些是產品密碼 policy；前端同名規則只改善 UX，server 仍是唯一安全裁決。
    private static final int MIN_PASSWORD_CHARACTERS = 8;
    private static final int MAX_PASSWORD_CHARACTERS = 32;
    private static final int MAX_BCRYPT_PASSWORD_BYTES = 72;
    private static final int REGISTRATION_NUMERIC_CODE_LENGTH = 6;
    private static final int REGISTRATION_LETTER_CODE_LENGTH = 5;
    // 排除 I、L、O，避免使用者在不同字型與手機螢幕間把相近字母誤認成數字。
    private static final char[] REGISTRATION_LETTER_ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ".toCharArray();

    private final UserAuthenticationRepository repository;
    private final RegistrationEmailBloomFilter registrationEmailBloomFilter;
    private final BCryptPasswordEncoder passwordEncoder;
    private final PasswordResetDeliveryPort passwordResetDelivery;
    private final RegistrationVerificationDeliveryPort registrationVerificationDelivery;
    private final LoginVerificationDeliveryPort loginVerificationDelivery;
    private final UserAuthenticationProperties properties;
    private final Clock clock;

    @Autowired
    public UserAuthenticationService(
        UserAuthenticationRepository repository,
        RegistrationEmailBloomFilter registrationEmailBloomFilter,
        BCryptPasswordEncoder passwordEncoder,
        PasswordResetDeliveryPort passwordResetDelivery,
        RegistrationVerificationDeliveryPort registrationVerificationDelivery,
        LoginVerificationDeliveryPort loginVerificationDelivery,
        UserAuthenticationProperties properties
    ) {
        this(repository, registrationEmailBloomFilter, passwordEncoder, passwordResetDelivery, registrationVerificationDelivery,
            loginVerificationDelivery, properties, Clock.systemUTC());
    }

    UserAuthenticationService(
        UserAuthenticationRepository repository,
        RegistrationEmailBloomFilter registrationEmailBloomFilter,
        BCryptPasswordEncoder passwordEncoder,
        PasswordResetDeliveryPort passwordResetDelivery,
        LoginVerificationDeliveryPort loginVerificationDelivery,
        UserAuthenticationProperties properties,
        Clock clock
    ) {
        this(repository, registrationEmailBloomFilter, passwordEncoder, passwordResetDelivery,
            new RegistrationVerificationDeliveryPort() {
                @Override public boolean isAvailable() { return false; }
                @Override public void deliver(String email, RegistrationVerificationSecret secret) {
                    throw new IllegalStateException("Registration verification delivery is unavailable");
                }
            }, loginVerificationDelivery, properties, clock);
    }

    UserAuthenticationService(
        UserAuthenticationRepository repository,
        RegistrationEmailBloomFilter registrationEmailBloomFilter,
        BCryptPasswordEncoder passwordEncoder,
        PasswordResetDeliveryPort passwordResetDelivery,
        RegistrationVerificationDeliveryPort registrationVerificationDelivery,
        LoginVerificationDeliveryPort loginVerificationDelivery,
        UserAuthenticationProperties properties,
        Clock clock
    ) {
        this.repository = repository;
        this.registrationEmailBloomFilter = registrationEmailBloomFilter;
        this.passwordEncoder = passwordEncoder;
        this.passwordResetDelivery = passwordResetDelivery;
        this.registrationVerificationDelivery = registrationVerificationDelivery;
        this.loginVerificationDelivery = loginVerificationDelivery;
        this.properties = properties;
        this.clock = clock;
    }

    /**
     * 發起註冊 email 雙驗證。
     *
     * <p>此步驟只保存短時效申請與 BCrypt password hash，絕不建立使用者、受信任裝置或 session；SMTP
     * 未配置時必須 fail closed，避免產生使用者無法完成的半成品帳號。</p>
     */
    @Transactional
    public RegistrationVerificationRequested requestRegistrationVerification(String email, String displayName, String password) {
        if (!registrationVerificationDelivery.isAvailable()) {
            throw new ApiException(ApiErrorCode.SERVICE_UNAVAILABLE);
        }
        String normalizedEmail = normalizeEmail(email);
        String normalizedDisplayName = validateDisplayName(displayName);
        validatePassword(password);

        // Bloom 命中只能表示「可能重複」；false positive 必須以資料庫查詢消除，不能錯拒新使用者。
        if (registrationEmailBloomFilter.mightContain(normalizedEmail) && repository.userExistsByEmail(normalizedEmail)) {
            throw new ApiException(ApiErrorCode.EMAIL_ALREADY_REGISTERED);
        }
        RegistrationVerificationSecret secret = createRegistrationVerificationSecret();
        AuthenticatedUser user = new AuthenticatedUser(UUID.randomUUID().toString(), normalizedEmail, normalizedDisplayName);
        PendingRegistration pending = new PendingRegistration(
            secret.registrationId(), user, passwordEncoder.encode(password), secret.numericCodeDigest(), secret.letterCodeDigest(),
            0, Instant.now(clock).plus(properties.getRegistrationVerification().getTtl())
        );
        // 同一地址重寄時由 repository 原子淘汰舊碼；寄送失敗會讓 transaction rollback，不留下失聯申請。
        repository.upsertRegistrationVerification(pending);
        registrationVerificationDelivery.deliver(normalizedEmail, secret);
        return new RegistrationVerificationRequested(secret.registrationId());
    }

    /**
     * 同時驗證 email 內的六位數字碼與五位英文字母碼，成功後才建立第一個 session。
     *
     * <p>讀取時採 row lock，確保同一申請只可完成一次；任一 code 錯誤都消耗一次嘗試，達上限後必須重新
     * 發起註冊。兩個 digest 使用 constant-time 比較，不能因回應時間透露哪一組已經正確。</p>
     */
    @Transactional
    public AuthenticationResult completeRegistrationVerification(
        UUID registrationId,
        String numericCode,
        String letterCode,
        LoginRequestMetadata metadata
    ) {
        PendingRegistration pending = repository.lockActiveRegistrationVerification(registrationId)
            .orElseThrow(() -> new ApiException(ApiErrorCode.AUTHENTICATION_ERROR));
        boolean verified = codeMatches(pending.numericCodeDigest(), normalizeNumericRegistrationCode(numericCode))
            & codeMatches(pending.letterCodeDigest(), normalizeLetterRegistrationCode(letterCode));
        if (!verified) {
            repository.recordRegistrationVerificationFailure(registrationId, properties.getRegistrationVerification().getMaxAttempts());
            throw new ApiException(ApiErrorCode.AUTHENTICATION_ERROR);
        }
        try {
            // users.email 的 unique index 仍是併發下唯一最終裁決；不能因 pending request 存在而跳過它。
            repository.createUser(pending.user(), pending.passwordHash());
        } catch (DataIntegrityViolationException exception) {
            throw new ApiException(ApiErrorCode.EMAIL_ALREADY_REGISTERED, exception, null);
        }
        registrationEmailBloomFilter.add(pending.user().email());
        DeviceSecret device = createDeviceSecret();
        repository.createTrustedDevice(device.deviceId(), pending.user().userId(), device.secretDigest(), metadata);
        repository.consumeRegistrationVerification(registrationId);
        return new AuthenticationResult(
            pending.user(), createSession(pending.user().userId(), metadata, device.deviceId()), device
        );
    }

    /**
     * 以 email/password 驗證登入。
     *
     * <p>只有 cookie 與 browser fingerprint 均相符的可信裝置可以立即建立 session。未知裝置是否需要
     * email 確認由帳戶本人開關決定；關閉時仍會建立可稽核的受信任裝置與 session，不會把裝置資料遺失。</p>
     */
    @Transactional
    public LoginResult login(String email, String password, DeviceSecret existingDevice, LoginRequestMetadata metadata) {
        String normalizedEmail = normalizeEmail(email);
        validatePasswordInput(password);
        Optional<PasswordCredential> credential = repository.findPasswordCredentialByEmail(normalizedEmail);
        if (credential.isEmpty() || !passwordEncoder.matches(password, credential.get().passwordHash())) {
            // 帳號不存在與密碼錯誤必須使用同一對外錯誤，避免帳號枚舉。
            throw new ApiException(ApiErrorCode.AUTHENTICATION_ERROR);
        }
        AuthenticatedUser user = credential.get().user();
        if (existingDevice != null) {
            Optional<TrustedLoginDevice> trustedDevice = repository.findActiveTrustedDevice(
                user.userId(), existingDevice.deviceId(), existingDevice.secretDigest()
            );
            if (trustedDevice.isPresent()
                && trustedDevice.get().userAgentDigest().equals(metadata.userAgentDigest())) {
                repository.touchTrustedDevice(trustedDevice.get().deviceId(), metadata);
                return LoginResult.authenticated(new AuthenticationResult(
                    user, createSession(user.userId(), metadata, trustedDevice.get().deviceId()), null
                ));
            }
        }

        LoginSecuritySettings securitySettings = repository.findActiveLoginSecuritySettings(user.userId())
            .orElseThrow(() -> new ApiException(ApiErrorCode.AUTHENTICATION_ERROR));
        if (!securitySettings.newDeviceLoginEmailNotificationEnabled()
            || !repository.hasActiveBoundLoginDevices(user.userId())) {
            // 沒有任何既有裝置時沒有可供比對的安全基線；直接建立第一個可稽核裝置，不寄出無意義通知。
            DeviceSecret newDevice = createDeviceSecret();
            repository.createTrustedDevice(newDevice.deviceId(), user.userId(), newDevice.secretDigest(), metadata);
            return LoginResult.authenticated(new AuthenticationResult(
                user, createSession(user.userId(), metadata, newDevice.deviceId()), newDevice
            ));
        }

        if (!loginVerificationDelivery.isAvailable()) {
            // 沒有可用 email delivery 時絕不能因便利而把未知裝置直接視為可信。
            throw new ApiException(ApiErrorCode.SERVICE_UNAVAILABLE);
        }
        PendingLoginVerificationSecret pending = createPendingLoginVerificationSecret();
        LoginVerificationSecret approval = createLoginVerificationSecret(pending.verificationRequestId());
        repository.createLoginVerification(
            pending.verificationRequestId(), user.userId(), pending.secretDigest(), approval.secretDigest(),
            pending.candidateDevice().deviceId(), pending.candidateDevice().secretDigest(), metadata,
            Instant.now(clock).plus(properties.getLoginVerification().getTtl())
        );
        // adapter 寄送失敗會使同一 transaction rollback，不留下使用者永遠收不到的 pending request。
        loginVerificationDelivery.deliver(user, metadata, approval);
        return LoginResult.verificationRequired(pending);
    }

    /** 取得目前 session 的使用者投影；失效、撤銷與不存在都採同一失敗語意。 */
    @Transactional
    public AuthenticatedUser authenticate(SessionSecret sessionSecret) {
        return repository.findActiveSession(sessionSecret.sessionId(), sessionSecret.secretDigest())
            .orElseThrow(() -> new ApiException(ApiErrorCode.AUTHENTICATION_ERROR));
    }

    /**
     * 讀取目前使用者自己的成功登入歷程。
     *
     * <p>維持 primary transaction，讓剛建立的 session 能立即出現在帳戶頁。before／after／anchor 都是
     * 使用者已可見的成功登入時間；它們只決定本人的讀取窗口，不得攜帶 userId 或 session 身分。</p>
     */
    @Transactional
    public LoginHistoryPage getLoginHistory(
        AuthenticatedUser authenticatedUser,
        Integer requestedLimit,
        Instant before,
        Instant after,
        Instant anchor
    ) {
        if (countNonNull(before, after, anchor) > 1) {
            throw new ApiException(ApiErrorCode.VALIDATION_ERROR);
        }
        int limit = normalizeLoginHistoryLimit(requestedLimit);
        int probeLimit = limit + 1;

        if (after != null) {
            List<LoginHistoryEntry> records = repository.findLoginHistoryAfter(authenticatedUser.userId(), after, probeLimit);
            boolean hasNewer = records.size() > limit;
            List<LoginHistoryEntry> page = truncate(records, limit);
            // SQL 以 ASC 找到最靠近 cursor 的較新紀錄；回應仍統一以最新在前的 DESC 順序呈現。
            Collections.reverse(page);
            return new LoginHistoryPage(page, true, hasNewer);
        }

        List<LoginHistoryEntry> records = anchor != null
            ? repository.findLoginHistoryOnOrBefore(authenticatedUser.userId(), anchor, probeLimit)
            : before != null
                ? repository.findLoginHistoryBefore(authenticatedUser.userId(), before, probeLimit)
                : repository.findLoginHistory(authenticatedUser.userId(), probeLimit);
        boolean hasOlder = records.size() > limit;
        List<LoginHistoryEntry> page = truncate(records, limit);
        boolean hasNewer = anchor != null
            ? repository.hasLoginHistoryAfter(authenticatedUser.userId(), anchor)
            : before != null && repository.hasLoginHistoryAfter(authenticatedUser.userId(), before);
        return new LoginHistoryPage(page, hasOlder, hasNewer);
    }

    private static int normalizeLoginHistoryLimit(Integer requestedLimit) {
        int limit = requestedLimit == null ? DEFAULT_LOGIN_HISTORY_PAGE_SIZE : requestedLimit;
        if (limit < 1 || limit > MAX_LOGIN_HISTORY_PAGE_SIZE) {
            throw new ApiException(ApiErrorCode.VALIDATION_ERROR);
        }
        return limit;
    }

    private static int countNonNull(Object... values) {
        int count = 0;
        for (Object value : values) {
            if (value != null) count++;
        }
        return count;
    }

    private static List<LoginHistoryEntry> truncate(List<LoginHistoryEntry> records, int limit) {
        return new ArrayList<>(records.subList(0, Math.min(records.size(), limit)));
    }

    /** 取得目前登入者自己的基本 profile；不得由 caller 指定 userId。 */
    @Transactional
    public UserProfile getUserProfile(AuthenticatedUser authenticatedUser) {
        return repository.findActiveUserProfile(authenticatedUser.userId())
            .orElseThrow(() -> new ApiException(ApiErrorCode.AUTHENTICATION_ERROR));
    }

    /** 個人中心只可讀取目前登入者自己的新裝置通知偏好與受信任裝置快照。 */
    @Transactional
    public LoginSecurityOverview getLoginSecurityOverview(AuthenticatedUser authenticatedUser) {
        LoginSecuritySettings settings = repository.findActiveLoginSecuritySettings(authenticatedUser.userId())
            .orElseThrow(() -> new ApiException(ApiErrorCode.AUTHENTICATION_ERROR));
        List<BoundLoginDevice> devices = repository.findActiveBoundLoginDevices(authenticatedUser.userId());
        return new LoginSecurityOverview(settings, devices);
    }

    /**
     * 變更新裝置通知開關。
     *
     * <p>owner 只能從已驗證 session 取得；不可讓 client 指定 userId。關閉只停止 email 核准流程，並不會
     * 刪除既有裝置、session 或登入紀錄。</p>
     */
    @Transactional
    public LoginSecuritySettings updateNewDeviceLoginEmailNotificationEnabled(AuthenticatedUser authenticatedUser, boolean enabled) {
        if (!repository.updateNewDeviceLoginEmailNotificationEnabled(authenticatedUser.userId(), enabled)) {
            throw new ApiException(ApiErrorCode.AUTHENTICATION_ERROR);
        }
        return new LoginSecuritySettings(enabled);
    }

    /**
     * 移除目前帳戶的一個綁定裝置。
     *
     * <p>撤銷 device cookie 本身不會讓已建立 session 自動失效，因此同一 transaction 必須一併撤銷這個
     * device 的所有 active session；若使用者移除目前裝置，下一次 API 請求就會要求重新登入。</p>
     */
    @Transactional
    public void removeBoundLoginDevice(AuthenticatedUser authenticatedUser, UUID deviceId) {
        if (!repository.revokeBoundLoginDevice(authenticatedUser.userId(), deviceId)) {
            throw new ApiException(ApiErrorCode.AUTHENTICATION_ERROR);
        }
        repository.revokeSessionsForDevice(authenticatedUser.userId(), deviceId);
    }

    /**
     * 更新目前登入者的顯示名稱。
     *
     * <p>session principal 是唯一的 owner 來源，不能接受前端傳入 userId。若帳號在驗證與更新的間隔被
     * 停用，條件式 UPDATE 必須失敗，避免舊 principal 對已停用帳號保留寫入能力。</p>
     */
    @Transactional
    public AuthenticatedUser updateDisplayName(AuthenticatedUser authenticatedUser, String displayName) {
        String normalizedDisplayName = validateDisplayName(displayName);
        if (!repository.updateDisplayName(authenticatedUser.userId(), normalizedDisplayName)) {
            throw new ApiException(ApiErrorCode.AUTHENTICATION_ERROR);
        }
        return new AuthenticatedUser(authenticatedUser.userId(), authenticatedUser.email(), normalizedDisplayName);
    }

    /** 登出是可重試操作；不存在或已撤銷的 session 不暴露額外資訊。 */
    @Transactional
    public void logout(SessionSecret sessionSecret) {
        repository.revokeSession(sessionSecret.sessionId());
    }

    /**
     * 變更密碼後撤銷所有舊 session，再建立新 session，避免既有裝置保留舊密碼授權。
     */
    @Transactional
    public AuthenticationResult changePassword(
        SessionSecret sessionSecret,
        String currentPassword,
        String newPassword,
        DeviceSecret device,
        LoginRequestMetadata metadata
    ) {
        AuthenticatedUser user = authenticate(sessionSecret);
        validatePasswordInput(currentPassword);
        validatePassword(newPassword);
        PasswordCredential credential = repository.findPasswordCredentialByEmail(user.email())
            .orElseThrow(() -> new ApiException(ApiErrorCode.AUTHENTICATION_ERROR));
        if (!passwordEncoder.matches(currentPassword, credential.passwordHash())) {
            throw new ApiException(ApiErrorCode.AUTHENTICATION_ERROR);
        }

        repository.updatePasswordHash(user.userId(), passwordEncoder.encode(newPassword));
        repository.revokeAllSessions(user.userId());
        UUID deviceId = device == null ? null : device.deviceId();
        return new AuthenticationResult(user, createSession(user.userId(), metadata, deviceId), null);
    }

    /** email 確認頁只能將 PENDING 請求決定一次；它本身不建立 session。 */
    @Transactional
    public LoginVerificationState decideLoginVerification(String token, boolean approved) {
        LoginVerificationRequest request = repository.lockActiveLoginVerificationByApprovalToken(digestSecret(token))
            .orElseThrow(() -> new ApiException(ApiErrorCode.AUTHENTICATION_ERROR));
        if (request.state() != LoginVerificationState.PENDING) {
            return request.state();
        }
        if (!repository.decideLoginVerification(request.verificationRequestId(), approved)) {
            throw new ApiException(ApiErrorCode.AUTHENTICATION_ERROR);
        }
        return approved ? LoginVerificationState.APPROVED : LoginVerificationState.REJECTED;
    }

    /**
     * 在 email 確認頁消耗核准 token，並直接在該瀏覽器建立 session。
     *
     * <p>Yes 不是 GET：確認頁必須以 POST 明確送出。token 僅保存摘要且在同一筆 `FOR UPDATE` request 中
     * 原子消耗；每次成功 Yes 都生成新的 device cookie，因此綁定清單與登入紀錄會反映實際點選的瀏覽器。</p>
     */
    @Transactional
    public LoginVerificationCompletion completeLoginVerificationByEmail(
        String token,
        boolean approved,
        LoginRequestMetadata metadata
    ) {
        LoginVerificationRequest request = repository.lockActiveLoginVerificationByApprovalToken(digestSecret(token))
            .orElseThrow(() -> new ApiException(ApiErrorCode.AUTHENTICATION_ERROR));
        if (!approved) {
            if (request.state() == LoginVerificationState.PENDING
                && !repository.decideLoginVerification(request.verificationRequestId(), false)) {
                throw new ApiException(ApiErrorCode.AUTHENTICATION_ERROR);
            }
            return LoginVerificationCompletion.rejected();
        }
        if (request.state() == LoginVerificationState.PENDING
            && !repository.decideLoginVerification(request.verificationRequestId(), true)) {
            throw new ApiException(ApiErrorCode.AUTHENTICATION_ERROR);
        }
        if (request.state() == LoginVerificationState.REJECTED
            || !repository.consumeApprovedLoginVerification(request.verificationRequestId())) {
            throw new ApiException(ApiErrorCode.AUTHENTICATION_ERROR);
        }

        DeviceSecret emailConfirmedDevice = createDeviceSecret();
        repository.createTrustedDevice(
            emailConfirmedDevice.deviceId(), request.user().userId(), emailConfirmedDevice.secretDigest(), metadata
        );
        return LoginVerificationCompletion.authenticated(new AuthenticationResult(
            request.user(), createSession(request.user().userId(), metadata, emailConfirmedDevice.deviceId()), emailConfirmedDevice
        ));
    }

    /**
     * 由原始登入瀏覽器消耗已核准請求並建立 session。
     *
     * <p>pending cookie 的高熵秘密與原始 browser fingerprint 都必須相符。email token 就算被取得，最多只
     * 能改變核准決定，不能單獨在 email 閱讀裝置建立登入 session。</p>
     */
    @Transactional
    public LoginVerificationCompletion completeLoginVerification(
        PendingLoginVerificationSecret pending,
        LoginRequestMetadata metadata
    ) {
        LoginVerificationRequest request = repository.lockActiveLoginVerificationByPendingToken(
            pending.verificationRequestId(), pending.secretDigest()
        ).orElseThrow(() -> new ApiException(ApiErrorCode.AUTHENTICATION_ERROR));
        if (request.state() == LoginVerificationState.PENDING) {
            return LoginVerificationCompletion.pending();
        }
        if (request.state() == LoginVerificationState.REJECTED) {
            return LoginVerificationCompletion.rejected();
        }
        if (!request.metadata().userAgentDigest().equals(metadata.userAgentDigest())
            || !request.candidateDeviceId().equals(pending.candidateDevice().deviceId())
            || !request.candidateDeviceTokenDigest().equals(pending.candidateDevice().secretDigest())) {
            // 不能只因 email 點選核准就讓被複製的 pending cookie 在另一個 browser 建立 session。
            throw new ApiException(ApiErrorCode.AUTHENTICATION_ERROR);
        }
        if (!repository.consumeApprovedLoginVerification(request.verificationRequestId())) {
            throw new ApiException(ApiErrorCode.AUTHENTICATION_ERROR);
        }
        repository.createTrustedDevice(
            pending.candidateDevice().deviceId(), request.user().userId(), pending.candidateDevice().secretDigest(), metadata
        );
        return LoginVerificationCompletion.authenticated(new AuthenticationResult(
            request.user(), createSession(request.user().userId(), metadata, pending.candidateDevice().deviceId()),
            pending.candidateDevice()
        ));
    }

    /**
     * 建立一次性重設憑證並交給受控寄信 adapter。
     *
     * <p>SMTP 未配置時先 fail closed，不能產生使用者永遠收不到的半成品憑證。地址不存在時仍回傳
     * 成功語意，避免外部透過此 endpoint 枚舉註冊狀態。</p>
     */
    @Transactional
    public void requestPasswordReset(String email) {
        if (!passwordResetDelivery.isAvailable()) {
            throw new ApiException(ApiErrorCode.SERVICE_UNAVAILABLE);
        }
        String normalizedEmail = normalizeEmailForReset(email);
        Optional<AuthenticatedUser> user = repository.findActiveUserByEmail(normalizedEmail);
        if (user.isEmpty()) {
            return;
        }

        PasswordResetSecret secret = createPasswordResetSecret();
        repository.createPasswordReset(
            secret.requestId(), user.get().userId(), secret.secretDigest(),
            Instant.now(clock).plus(properties.getPasswordReset().getTtl())
        );
        // 寄送失敗時 transaction 會 rollback，避免保留未送達且無法使用者取得的 token。
        passwordResetDelivery.deliver(user.get(), secret);
    }

    /** 使用 email 寄送的一次性 token 設定新密碼，並撤銷所有既有 session。 */
    @Transactional
    public void resetPassword(String token, String newPassword) {
        validatePassword(newPassword);
        ResettableCredential resettableCredential = repository.lockActivePasswordReset(digestSecret(token))
            .orElseThrow(() -> new ApiException(ApiErrorCode.AUTHENTICATION_ERROR));
        repository.updatePasswordHash(resettableCredential.user().userId(), passwordEncoder.encode(newPassword));
        repository.revokeAllSessions(resettableCredential.user().userId());
        repository.consumePasswordReset(resettableCredential.requestId());
    }

    private SessionSecret createSession(String userId, LoginRequestMetadata metadata, UUID deviceId) {
        SessionSecret session = createSessionSecret();
        repository.createSession(
            session.sessionId(), userId, session.secretDigest(), Instant.now(clock).plus(properties.getSessionTtl()),
            deviceId, metadata
        );
        return session;
    }

    private static String normalizeEmail(String email) {
        if (email == null) {
            throw new ApiException(ApiErrorCode.VALIDATION_ERROR);
        }
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        if (normalized.length() > 320 || !normalized.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new ApiException(ApiErrorCode.VALIDATION_ERROR);
        }
        return normalized;
    }

    private static String normalizeEmailForReset(String email) {
        // 忘記密碼只能採泛化結果，格式錯誤也不應洩漏該地址是否存在。
        try {
            return normalizeEmail(email);
        } catch (ApiException exception) {
            return "invalid-reset-address@invalid.local";
        }
    }

    private static String validateDisplayName(String displayName) {
        if (displayName == null) {
            throw new ApiException(ApiErrorCode.VALIDATION_ERROR);
        }
        String normalized = displayName.trim();
        if (normalized.length() < 2 || normalized.length() > 128) {
            throw new ApiException(ApiErrorCode.VALIDATION_ERROR);
        }
        return normalized;
    }

    private static void validatePassword(String password) {
        validatePasswordInput(password);
        int byteLength = password.getBytes(StandardCharsets.UTF_8).length;
        if (password.length() < MIN_PASSWORD_CHARACTERS || password.length() > MAX_PASSWORD_CHARACTERS
            || byteLength > MAX_BCRYPT_PASSWORD_BYTES) {
            // BCrypt 只安全處理前 72 bytes；產品長度上限為 32 字元，兩者都必須拒絕而非靜默截斷。
            throw new ApiException(ApiErrorCode.VALIDATION_ERROR);
        }
    }

    private static void validatePasswordInput(String password) {
        if (password == null || password.isBlank()
            || password.getBytes(StandardCharsets.UTF_8).length > MAX_BCRYPT_PASSWORD_BYTES) {
            throw new ApiException(ApiErrorCode.VALIDATION_ERROR);
        }
    }

    private static SessionSecret createSessionSecret() {
        String secret = randomSecret();
        return new SessionSecret(UUID.randomUUID(), secret, digestSecret(secret));
    }

    private static DeviceSecret createDeviceSecret() {
        String secret = randomSecret();
        return new DeviceSecret(UUID.randomUUID(), secret, digestSecret(secret));
    }

    private static PasswordResetSecret createPasswordResetSecret() {
        String secret = randomSecret();
        return new PasswordResetSecret(UUID.randomUUID(), secret, digestSecret(secret));
    }

    private static RegistrationVerificationSecret createRegistrationVerificationSecret() {
        String numericCode = String.format(Locale.ROOT, "%0" + REGISTRATION_NUMERIC_CODE_LENGTH + "d",
            SECURE_RANDOM.nextInt(1_000_000));
        StringBuilder letterCode = new StringBuilder(REGISTRATION_LETTER_CODE_LENGTH);
        for (int index = 0; index < REGISTRATION_LETTER_CODE_LENGTH; index++) {
            letterCode.append(REGISTRATION_LETTER_ALPHABET[SECURE_RANDOM.nextInt(REGISTRATION_LETTER_ALPHABET.length)]);
        }
        String letters = letterCode.toString();
        return new RegistrationVerificationSecret(
            UUID.randomUUID(), numericCode, letters, digestSecret(numericCode), digestSecret(letters)
        );
    }

    private static String normalizeNumericRegistrationCode(String value) {
        if (value == null) return "-";
        String normalized = value.trim();
        return normalized.matches("\\d{" + REGISTRATION_NUMERIC_CODE_LENGTH + "}") ? normalized : "-";
    }

    private static String normalizeLetterRegistrationCode(String value) {
        if (value == null) return "-";
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return normalized.matches("[A-Z]{" + REGISTRATION_LETTER_CODE_LENGTH + "}") ? normalized : "-";
    }

    private static boolean codeMatches(String expectedDigest, String suppliedCode) {
        // 格式錯誤會先正規化為非空的無效標記並仍參與比較，避免走出可觀察的 timing 分支。
        return MessageDigest.isEqual(
            expectedDigest.getBytes(StandardCharsets.UTF_8), digestSecret(suppliedCode).getBytes(StandardCharsets.UTF_8)
        );
    }

    private static LoginVerificationSecret createLoginVerificationSecret(UUID verificationRequestId) {
        String secret = randomSecret();
        return new LoginVerificationSecret(verificationRequestId, secret, digestSecret(secret));
    }

    private static PendingLoginVerificationSecret createPendingLoginVerificationSecret() {
        String secret = randomSecret();
        return new PendingLoginVerificationSecret(UUID.randomUUID(), secret, digestSecret(secret), createDeviceSecret());
    }

    private static String randomSecret() {
        byte[] bytes = new byte[SECRET_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String digestSecret(String secret) {
        if (secret == null || secret.isBlank() || secret.length() > 512) {
            throw new ApiException(ApiErrorCode.AUTHENTICATION_ERROR);
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            // Java 平台必須提供 SHA-256；若不存在，不能降級為較弱的摘要。
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    /**
     * 從 Cookie 還原 session 查詢材料。
     *
     * <p>格式不正確一律視同未認證，避免 controller 自行切割後產生例外訊息或不一致的解析規則。</p>
     */
    public static SessionSecret parseCookieValue(String cookieValue) {
        if (cookieValue == null || cookieValue.length() > 512) {
            throw new ApiException(ApiErrorCode.AUTHENTICATION_ERROR);
        }
        int delimiter = cookieValue.indexOf('.');
        if (delimiter <= 0 || delimiter != cookieValue.lastIndexOf('.') || delimiter == cookieValue.length() - 1) {
            throw new ApiException(ApiErrorCode.AUTHENTICATION_ERROR);
        }
        try {
            UUID sessionId = UUID.fromString(cookieValue.substring(0, delimiter));
            String secret = cookieValue.substring(delimiter + 1);
            return new SessionSecret(sessionId, secret, digestSecret(secret));
        } catch (IllegalArgumentException exception) {
            throw new ApiException(ApiErrorCode.AUTHENTICATION_ERROR);
        }
    }

    /** 損壞或過期裝置 cookie 一律由 controller 當作未知裝置，不得例外放行。 */
    public static DeviceSecret parseDeviceCookieValue(String cookieValue) {
        String[] parts = parseCookieParts(cookieValue, 2);
        try {
            return new DeviceSecret(UUID.fromString(parts[0]), parts[1], digestSecret(parts[1]));
        } catch (IllegalArgumentException exception) {
            throw new ApiException(ApiErrorCode.AUTHENTICATION_ERROR);
        }
    }

    /** pending cookie 同時綁定候選裝置，不能讓 email token 或單一 cookie 單獨完成登入。 */
    public static PendingLoginVerificationSecret parsePendingLoginVerificationCookieValue(String cookieValue) {
        String[] parts = parseCookieParts(cookieValue, 4);
        try {
            DeviceSecret candidateDevice = new DeviceSecret(UUID.fromString(parts[2]), parts[3], digestSecret(parts[3]));
            return new PendingLoginVerificationSecret(
                UUID.fromString(parts[0]), parts[1], digestSecret(parts[1]), candidateDevice
            );
        } catch (IllegalArgumentException exception) {
            throw new ApiException(ApiErrorCode.AUTHENTICATION_ERROR);
        }
    }

    private static String[] parseCookieParts(String cookieValue, int expectedParts) {
        if (cookieValue == null || cookieValue.length() > 1024) {
            throw new ApiException(ApiErrorCode.AUTHENTICATION_ERROR);
        }
        String[] parts = cookieValue.split("\\.", -1);
        if (parts.length != expectedParts) {
            throw new ApiException(ApiErrorCode.AUTHENTICATION_ERROR);
        }
        for (String part : parts) {
            if (part.isBlank()) throw new ApiException(ApiErrorCode.AUTHENTICATION_ERROR);
        }
        return parts;
    }

    /** 認證結果只供 controller 建立 cookie 與安全使用者投影，不含 password/token。 */
    public record AuthenticationResult(AuthenticatedUser user, SessionSecret session, DeviceSecret device) { }

    /** 註冊信已排入受控 SMTP 後，browser 只能得到無秘密的 request id 用於下一步驗證。 */
    public record RegistrationVerificationRequested(UUID registrationId) { }

    /** 登入只有立即認證或等待 email 確認兩種結果，避免未知裝置默默取得 session。 */
    public record LoginResult(AuthenticationResult authentication, PendingLoginVerificationSecret pendingVerification) {
        static LoginResult authenticated(AuthenticationResult authentication) {
            return new LoginResult(authentication, null);
        }

        static LoginResult verificationRequired(PendingLoginVerificationSecret pendingVerification) {
            return new LoginResult(null, pendingVerification);
        }

        public boolean requiresVerification() {
            return pendingVerification != null;
        }
    }

    /** 原始登入瀏覽器輪詢的狀態；PENDING 不建立 session，REJECTED 永遠不可轉為已登入。 */
    public record LoginVerificationCompletion(LoginVerificationState state, AuthenticationResult authentication) {
        static LoginVerificationCompletion pending() {
            return new LoginVerificationCompletion(LoginVerificationState.PENDING, null);
        }

        static LoginVerificationCompletion rejected() {
            return new LoginVerificationCompletion(LoginVerificationState.REJECTED, null);
        }

        static LoginVerificationCompletion authenticated(AuthenticationResult authentication) {
            return new LoginVerificationCompletion(LoginVerificationState.APPROVED, authentication);
        }
    }

    /** 個人中心的安全資料只限於設定與去敏裝置清單。 */
    public record LoginSecurityOverview(LoginSecuritySettings settings, List<BoundLoginDevice> devices) { }
}
