package com.lumix.user.auth.mail;

import com.lumix.user.auth.application.LoginVerificationDeliveryPort;
import com.lumix.user.auth.config.UserAuthenticationProperties;
import com.lumix.user.auth.domain.AuthenticatedUser;
import com.lumix.user.auth.domain.LoginRequestMetadata;
import com.lumix.user.auth.domain.LoginVerificationSecret;
import java.time.format.DateTimeFormatter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/**
 * SMTP 新裝置登入確認 adapter。
 *
 * <p>與密碼重設共用同一個受控 SMTP／公開網址設定；email 連結只打開前端確認頁，不以 GET 改變核准狀態，
 * 避免郵件掃描器或預覽服務意外放行登入。</p>
 */
@Component
@Profile("infrastructure")
@ConditionalOnProperty(prefix = "lumix.auth.password-reset", name = "smtp-enabled", havingValue = "true")
public class SmtpLoginVerificationDelivery implements LoginVerificationDeliveryPort {

    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final String publicBaseUrl;

    public SmtpLoginVerificationDelivery(
        JavaMailSender mailSender,
        UserAuthenticationProperties properties
    ) {
        this.mailSender = mailSender;
        this.fromAddress = properties.getPasswordReset().getFromAddress();
        this.publicBaseUrl = removeTrailingSlash(properties.getPasswordReset().getPublicBaseUrl());
        Assert.hasText(fromAddress, "lumix.auth.passwordReset.fromAddress is required when SMTP is enabled");
        Assert.isTrue(SmtpPasswordResetDelivery.isAllowedResetBaseUrl(
            publicBaseUrl, properties.getPasswordReset().isAllowLoopbackHttp()
        ), "lumix.auth.passwordReset.publicBaseUrl must be an allowed verification destination");
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void deliver(AuthenticatedUser user, LoginRequestMetadata metadata, LoginVerificationSecret secret) {
        String confirmationUrl = publicBaseUrl + "/login-verification?token=" + secret.secret();
        try {
            MimeMessageHelper message = new MimeMessageHelper(mailSender.createMimeMessage(), "UTF-8");
            message.setFrom(fromAddress);
            message.setTo(user.email());
            message.setSubject("LumiX 新裝置登入確認");
            message.setText(htmlMessage(metadata, confirmationUrl), true);
            mailSender.send(message.getMimeMessage());
        } catch (jakarta.mail.MessagingException exception) {
            // 寄件失敗必須拋出，讓 application transaction rollback，而不是保留無法使用者取得的請求。
            throw new IllegalStateException("Unable to compose login verification email", exception);
        }
    }

    private static String htmlMessage(LoginRequestMetadata metadata, String confirmationUrl) {
        String escapedIp = escapeHtml(metadata.ipAddress());
        String escapedDevice = escapeHtml(metadata.deviceLabel());
        String escapedUrl = escapeHtml(confirmationUrl);
        String issuedAt = DateTimeFormatter.ISO_INSTANT.format(java.time.Instant.now());
        return "<p>我們偵測到新的裝置嘗試登入您的 LumiX 帳戶。</p>"
            + "<p>IP 位址：<strong>" + escapedIp + "</strong><br>"
            + "裝置：<strong>" + escapedDevice + "</strong><br>"
            + "時間（UTC）：<strong>" + issuedAt + "</strong></p>"
            + "<p><a href=\"" + escapedUrl + "&amp;intent=yes\" "
            + "style=\"display:inline-block;padding:10px 16px;background:#2563eb;color:#ffffff;text-decoration:none;border-radius:6px\">Yes，允許登入</a> "
            + "<a href=\"" + escapedUrl + "&amp;intent=no\" "
            + "style=\"display:inline-block;padding:10px 16px;background:#b91c1c;color:#ffffff;text-decoration:none;border-radius:6px\">No，拒絕登入</a></p>"
            + "<p>按鈕會開啟確認頁，再由您明確確認 Yes 或 No；開啟連結本身不會改變登入狀態，避免郵件安全掃描器誤觸。</p>";
    }

    private static String escapeHtml(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
            .replace("\"", "&quot;").replace("'", "&#39;");
    }

    private static String removeTrailingSlash(String value) {
        if (value == null) return "";
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
