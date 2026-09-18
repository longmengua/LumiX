package com.lumix.admin.superadmin;

import com.lumix.user.auth.application.PasswordResetDeliveryPort;
import com.lumix.user.auth.config.UserAuthenticationProperties;
import com.lumix.user.auth.domain.AuthenticatedUser;
import com.lumix.user.auth.domain.PasswordResetSecret;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

/**
 * 唯一最高管理員的啟動 bootstrap。
 *
 * <p>設定信箱為空時刻意不建立帳號，讓沒有後台需求的受控本機環境可啟動。設定存在時，SMTP 必須可用；
 * 不能把一次性 token 寫入 log 或 API 來繞過受控 email 傳遞。</p>
 */
@Service
@Profile("infrastructure")
public class SuperAdminBootstrapService implements SuperAdminActivationPort {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int SECRET_BYTES = 32;

    private final SuperAdminRepository repository;
    private final SuperAdminProperties properties;
    private final PasswordResetDeliveryPort passwordResetDelivery;
    private final BCryptPasswordEncoder passwordEncoder;
    private final UserAuthenticationProperties authenticationProperties;
    private final Clock clock;

    @Autowired
    SuperAdminBootstrapService(
        SuperAdminRepository repository,
        SuperAdminProperties properties,
        PasswordResetDeliveryPort passwordResetDelivery,
        BCryptPasswordEncoder passwordEncoder,
        UserAuthenticationProperties authenticationProperties
    ) {
        this(repository, properties, passwordResetDelivery, passwordEncoder, authenticationProperties, Clock.systemUTC());
    }

    SuperAdminBootstrapService(
        SuperAdminRepository repository,
        SuperAdminProperties properties,
        PasswordResetDeliveryPort passwordResetDelivery,
        BCryptPasswordEncoder passwordEncoder,
        UserAuthenticationProperties authenticationProperties,
        Clock clock
    ) {
        this.repository = repository;
        this.properties = properties;
        this.passwordResetDelivery = passwordResetDelivery;
        this.passwordEncoder = passwordEncoder;
        this.authenticationProperties = authenticationProperties;
        this.clock = clock;
    }

    /**
     * 啟動時確認既有 principal 或建立待啟用 principal，並寄送唯一有效的設定密碼連結。
     *
     * <p>資料庫中已有 ACTIVE 最高管理員時絕不重寄或變更；設定 email 與其不符時 fail closed，避免部署
     * 設定遭竄改後把最高權限轉交給另一個地址。</p>
     */
    @Transactional
    public void bootstrap() {
        String configuredEmail = normalizeConfiguredEmail(properties.getEmail());
        if (configuredEmail.isEmpty()) {
            return;
        }
        Assert.isTrue(passwordResetDelivery.isAvailable(),
            "最高管理員信箱已設定，但受控 SMTP password-reset transport 尚未啟用");

        SuperAdminPrincipal principal = repository.lockSuperAdmin().orElse(null);
        if (principal != null) {
            if (!principal.user().email().equals(configuredEmail)) {
                throw new IllegalStateException("設定的最高管理員信箱與已建立的 principal 不一致");
            }
            if (principal.state() == SuperAdminPrincipal.State.ACTIVE) {
                return;
            }
            issueActivationLink(principal.user());
            return;
        }

        AuthenticatedUser user = repository.findUserByEmail(configuredEmail).orElseGet(() -> createBootstrapUser(configuredEmail));
        repository.createPendingSuperAdmin(user.userId());
        issueActivationLink(user);
    }

    /** 密碼 hash 寫入且既有 session 撤銷後才啟用 principal，避免取得後台權限卻仍沿用未知舊密碼。 */
    @Override
    public void activateIfPending(String userId) {
        repository.activatePendingSuperAdmin(userId);
    }

    /** 後台密碼復原前需再次確認 principal 與一般使用者帳號都仍為 ACTIVE。 */
    @Override
    public boolean isActiveSuperAdmin(String userId) {
        return repository.isActiveSuperAdmin(userId);
    }

    /**
     * 首次啟用連結會導向與既有復原相同的後台重設頁；兩者都必須先確認是既有最高管理員 principal。
     */
    @Override
    public boolean isPasswordResetEligibleSuperAdmin(String userId) {
        return repository.isPasswordResetEligibleSuperAdmin(userId);
    }

    private AuthenticatedUser createBootstrapUser(String email) {
        AuthenticatedUser user = new AuthenticatedUser("admin-" + UUID.randomUUID(), email, "最高管理員");
        // placeholder 僅為滿足既有 credential 非空約束；隨機原文立刻丟棄，唯一可用密碼必須由 email 連結設定。
        repository.createBootstrapUser(user, passwordEncoder.encode(randomSecret()));
        return user;
    }

    private void issueActivationLink(AuthenticatedUser user) {
        PasswordResetSecret secret = createPasswordResetSecret();
        repository.invalidateActivePasswordResets(user.userId());
        repository.createPasswordReset(
            secret.requestId(), user.userId(), secret.secretDigest(),
            Instant.now(clock).plus(authenticationProperties.getPasswordReset().getTtl())
        );
        repository.markActivationRequested(user.userId());
        // 寄送失敗會使 transaction rollback，確保資料庫不會留下使用者收不到的最新 token。
        passwordResetDelivery.deliverSuperAdminActivation(user, secret, properties.getLocale());
    }

    private static String normalizeConfiguredEmail(String email) {
        if (email == null || email.isBlank()) {
            return "";
        }
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        if (normalized.length() > 320 || !normalized.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalStateException("LUMIX_ADMIN_SUPER_ADMIN_EMAIL 格式無效");
        }
        return normalized;
    }

    private static PasswordResetSecret createPasswordResetSecret() {
        String secret = randomSecret();
        return new PasswordResetSecret(UUID.randomUUID(), secret, digest(secret));
    }

    private static String randomSecret() {
        byte[] bytes = new byte[SECRET_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String digest(String value) {
        try {
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
