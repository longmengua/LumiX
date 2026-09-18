package com.lumix.user.auth.mail;

import com.lumix.user.auth.application.PasswordResetDeliveryPort;
import com.lumix.user.auth.config.UserAuthenticationProperties;
import com.lumix.user.auth.domain.AuthenticatedUser;
import com.lumix.user.auth.domain.PasswordResetSecret;
import java.net.URI;
import java.util.Locale;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.beans.factory.annotation.Value;

/**
 * SMTP 重設通知 adapter。
 *
 * <p>啟用時必須指定 HTTPS public base URL。唯一例外是明確開啟的本機 loopback HTTP 開發模式，且只接受
 * localhost、127.0.0.1 或 ::1；這個 adapter 只寫出信件，不記錄 token 或完整 URL。正式部署仍需由
 * secret manager 注入 spring.mail 的帳密及 TLS 設定。</p>
 */
@Component
@Profile("infrastructure")
@ConditionalOnProperty(prefix = "lumix.auth.password-reset", name = "smtp-enabled", havingValue = "true")
public class SmtpPasswordResetDelivery implements PasswordResetDeliveryPort {

    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final String publicBaseUrl;
    private final String adminPublicBaseUrl;

    public SmtpPasswordResetDelivery(
        JavaMailSender mailSender,
        UserAuthenticationProperties properties,
        @Value("${spring.mail.host:}") String smtpHost,
        @Value("${spring.mail.properties.mail.smtp.starttls.enable:false}") boolean startTlsEnabled
    ) {
        this.mailSender = mailSender;
        this.fromAddress = properties.getPasswordReset().getFromAddress();
        this.publicBaseUrl = removeTrailingSlash(properties.getPasswordReset().getPublicBaseUrl());
        this.adminPublicBaseUrl = removeTrailingSlash(properties.getAdminPublicBaseUrl());
        Assert.hasText(fromAddress, "lumix.auth.passwordReset.fromAddress is required when SMTP is enabled");
        Assert.hasText(smtpHost, "spring.mail.host is required when SMTP is enabled");
        Assert.isTrue(startTlsEnabled, "SMTP STARTTLS must be enabled when password reset SMTP is enabled");
        Assert.isTrue(isAllowedResetBaseUrl(publicBaseUrl, properties.getPasswordReset().isAllowLoopbackHttp()),
            "lumix.auth.passwordReset.publicBaseUrl must use HTTPS, except explicitly enabled loopback HTTP development URLs");
        Assert.hasText(adminPublicBaseUrl, "lumix.auth.adminPublicBaseUrl is required when SMTP is enabled");
        Assert.isTrue(isAllowedResetBaseUrl(adminPublicBaseUrl, properties.getPasswordReset().isAllowLoopbackHttp()),
            "lumix.auth.adminPublicBaseUrl must use HTTPS, except explicitly enabled loopback HTTP development URLs");
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void deliver(AuthenticatedUser user, PasswordResetSecret secret) {
        send(user, secret, "LumiX 密碼重設通知", "我們收到您的密碼重設請求。請於有效時間內開啟下列連結：", publicBaseUrl, "/reset-password");
    }

    /** 最高管理員啟用不應偽裝成一般忘記密碼信，讓收件人能辨識高權限帳戶的設定動作。 */
    @Override
    public void deliverSuperAdminActivation(AuthenticatedUser user, PasswordResetSecret secret, Locale locale) {
        // bootstrap locale 由部署者指定且已在 properties fail-closed 驗證，不能採收件人 header 或 browser 猜測。
        if (Locale.TAIWAN.equals(locale)) {
            send(user, secret, "LumiX 最高管理員帳號啟用", "此信箱被設定為 LumiX 最高管理員。請於有效時間內設定密碼以完成啟用：", adminPublicBaseUrl, "/admin/reset-password");
            return;
        }
        send(user, secret, "LumiX Super Administrator Account Activation", "This email address has been designated as a LumiX super administrator. Set your password before the link expires to complete activation:", adminPublicBaseUrl, "/admin/reset-password");
    }

    /** 管理員重設連結需固定前往後台路由，避免在客戶端重設頁混用高權限帳號流程。 */
    @Override
    public void deliverSuperAdminPasswordRecovery(AuthenticatedUser user, PasswordResetSecret secret) {
        send(user, secret, "LumiX 後台密碼重設通知", "我們收到後台帳號的密碼重設請求。請於有效時間內開啟下列連結：", adminPublicBaseUrl, "/admin/reset-password");
    }

    private void send(
        AuthenticatedUser user,
        PasswordResetSecret secret,
        String subject,
        String introduction,
        String destinationBaseUrl,
        String resetPath
    ) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(user.email());
        message.setSubject(subject);
        message.setText(introduction + "\n" + destinationBaseUrl + resetPath + "?token=" + secret.secret());
        mailSender.send(message);
    }

    private static String removeTrailingSlash(String value) {
        if (value == null) {
            return "";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    /**
     * 限制 reset token 的瀏覽器傳輸目的地。
     *
     * <p>後端以 SMTP 寄送信件不代表 token 回傳時安全；使用者點擊連結後仍會由瀏覽器帶著 token 連到此 URL。
     * 因此只允許 HTTPS，或由顯式開關啟用且流量不離開同機的 loopback HTTP。</p>
     */
    static boolean isAllowedResetBaseUrl(String value, boolean allowLoopbackHttp) {
        try {
            URI uri = URI.create(value);
            if (uri.getHost() == null || uri.getUserInfo() != null || uri.getRawQuery() != null || uri.getRawFragment() != null) {
                return false;
            }
            if ("https".equalsIgnoreCase(uri.getScheme())) {
                return true;
            }
            return allowLoopbackHttp && "http".equalsIgnoreCase(uri.getScheme()) && isLoopbackHost(uri.getHost());
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private static boolean isLoopbackHost(String host) {
        String normalizedHost = host.toLowerCase(Locale.ROOT);
        return "localhost".equals(normalizedHost)
            || "127.0.0.1".equals(normalizedHost)
            || "::1".equals(normalizedHost);
    }
}
