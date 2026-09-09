package com.lumix.user.auth.application;

import com.lumix.api.error.ApiErrorCode;
import com.lumix.api.error.ApiException;
import com.lumix.user.auth.config.UserAuthenticationProperties;
import com.lumix.user.auth.domain.AuthenticatedUser;
import com.lumix.user.auth.domain.LoginHistoryEntry;
import com.lumix.user.auth.domain.PasswordCredential;
import com.lumix.user.auth.domain.PasswordResetSecret;
import com.lumix.user.auth.domain.ResettableCredential;
import com.lumix.user.auth.domain.SessionSecret;
import com.lumix.user.auth.persistence.UserAuthenticationRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
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
    private static final int LOGIN_HISTORY_LIMIT = 50;
    // 這些是產品密碼 policy；前端同名規則只改善 UX，server 仍是唯一安全裁決。
    private static final int MIN_PASSWORD_CHARACTERS = 8;
    private static final int MAX_PASSWORD_CHARACTERS = 32;
    private static final int MAX_BCRYPT_PASSWORD_BYTES = 72;

    private final UserAuthenticationRepository repository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final PasswordResetDeliveryPort passwordResetDelivery;
    private final UserAuthenticationProperties properties;
    private final Clock clock;

    @Autowired
    public UserAuthenticationService(
        UserAuthenticationRepository repository,
        BCryptPasswordEncoder passwordEncoder,
        PasswordResetDeliveryPort passwordResetDelivery,
        UserAuthenticationProperties properties
    ) {
        this(repository, passwordEncoder, passwordResetDelivery, properties, Clock.systemUTC());
    }

    UserAuthenticationService(
        UserAuthenticationRepository repository,
        BCryptPasswordEncoder passwordEncoder,
        PasswordResetDeliveryPort passwordResetDelivery,
        UserAuthenticationProperties properties,
        Clock clock
    ) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.passwordResetDelivery = passwordResetDelivery;
        this.properties = properties;
        this.clock = clock;
    }

    /** 建立可登入使用者與第一個 session；email 正規化避免同一地址重複註冊。 */
    @Transactional
    public AuthenticationResult register(String email, String displayName, String password) {
        String normalizedEmail = normalizeEmail(email);
        String normalizedDisplayName = validateDisplayName(displayName);
        validatePassword(password);
        AuthenticatedUser user = new AuthenticatedUser(UUID.randomUUID().toString(), normalizedEmail, normalizedDisplayName);

        try {
            repository.createUser(user, passwordEncoder.encode(password));
        } catch (DataIntegrityViolationException exception) {
            // 唯一索引才是併發註冊的最終裁決，不以先查後寫取代資料庫約束。
            throw new ApiException(ApiErrorCode.CONFLICT, exception, null);
        }
        return new AuthenticationResult(user, createSession(user.userId()));
    }

    /** 以 email/password 驗證後建立新的伺服器端 session。 */
    @Transactional
    public AuthenticationResult login(String email, String password) {
        String normalizedEmail = normalizeEmail(email);
        validatePasswordInput(password);
        Optional<PasswordCredential> credential = repository.findPasswordCredentialByEmail(normalizedEmail);
        if (credential.isEmpty() || !passwordEncoder.matches(password, credential.get().passwordHash())) {
            // 帳號不存在與密碼錯誤必須使用同一對外錯誤，避免帳號枚舉。
            throw new ApiException(ApiErrorCode.AUTHENTICATION_ERROR);
        }
        return new AuthenticationResult(credential.get().user(), createSession(credential.get().user().userId()));
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
     * <p>維持 primary transaction，讓剛建立的 session 能立即出現在帳戶頁，且固定上限避免帳戶頁成為
     * 無界認證資料查詢。呼叫者身分已由全域 API filter 驗證，這裡只接受其去敏後的 principal。</p>
     */
    @Transactional
    public List<LoginHistoryEntry> getLoginHistory(AuthenticatedUser authenticatedUser) {
        return repository.findLoginHistory(authenticatedUser.userId(), LOGIN_HISTORY_LIMIT);
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
    public AuthenticationResult changePassword(SessionSecret sessionSecret, String currentPassword, String newPassword) {
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
        return new AuthenticationResult(user, createSession(user.userId()));
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

    private SessionSecret createSession(String userId) {
        SessionSecret session = createSessionSecret();
        repository.createSession(
            session.sessionId(), userId, session.secretDigest(), Instant.now(clock).plus(properties.getSessionTtl())
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

    private static PasswordResetSecret createPasswordResetSecret() {
        String secret = randomSecret();
        return new PasswordResetSecret(UUID.randomUUID(), secret, digestSecret(secret));
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

    /** 認證結果只供 controller 建立 cookie 與安全使用者投影，不含 password/token。 */
    public record AuthenticationResult(AuthenticatedUser user, SessionSecret session) { }
}
