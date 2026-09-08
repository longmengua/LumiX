package com.lumix.user.auth.mail;

import com.lumix.user.auth.application.PasswordResetDeliveryPort;
import com.lumix.user.auth.config.UserAuthenticationProperties;
import com.lumix.user.auth.domain.AuthenticatedUser;
import com.lumix.user.auth.domain.PasswordResetSecret;
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
 * <p>啟用時必須指定 HTTPS public base URL；這個 adapter 只寫出信件，不記錄 token 或完整 URL。
 * 正式部署仍需由 secret manager 注入 spring.mail 的帳密及 TLS 設定。</p>
 */
@Component
@Profile("infrastructure")
@ConditionalOnProperty(prefix = "lumix.auth.password-reset", name = "smtp-enabled", havingValue = "true")
public class SmtpPasswordResetDelivery implements PasswordResetDeliveryPort {

    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final String publicBaseUrl;

    public SmtpPasswordResetDelivery(
        JavaMailSender mailSender,
        UserAuthenticationProperties properties,
        @Value("${spring.mail.host:}") String smtpHost,
        @Value("${spring.mail.properties.mail.smtp.starttls.enable:false}") boolean startTlsEnabled
    ) {
        this.mailSender = mailSender;
        this.fromAddress = properties.getPasswordReset().getFromAddress();
        this.publicBaseUrl = removeTrailingSlash(properties.getPasswordReset().getPublicBaseUrl());
        Assert.hasText(fromAddress, "lumix.auth.passwordReset.fromAddress is required when SMTP is enabled");
        Assert.hasText(smtpHost, "spring.mail.host is required when SMTP is enabled");
        Assert.isTrue(startTlsEnabled, "SMTP STARTTLS must be enabled when password reset SMTP is enabled");
        Assert.isTrue(publicBaseUrl.startsWith("https://"),
            "lumix.auth.passwordReset.publicBaseUrl must use HTTPS when SMTP is enabled");
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void deliver(AuthenticatedUser user, PasswordResetSecret secret) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(user.email());
        message.setSubject("LumiX 密碼重設通知");
        message.setText("我們收到您的密碼重設請求。請於有效時間內開啟下列連結：\n"
            + publicBaseUrl + "/reset-password?token=" + secret.secret());
        mailSender.send(message);
    }

    private static String removeTrailingSlash(String value) {
        if (value == null) {
            return "";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
