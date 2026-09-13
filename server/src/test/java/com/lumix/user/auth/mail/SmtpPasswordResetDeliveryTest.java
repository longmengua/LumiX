package com.lumix.user.auth.mail;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.lumix.user.auth.config.UserAuthenticationProperties;
import com.lumix.user.auth.domain.AuthenticatedUser;
import com.lumix.user.auth.domain.PasswordResetSecret;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

/** 密碼重設連結 transport boundary 的測試；避免未來不慎把一般 HTTP 網址加入例外。 */
class SmtpPasswordResetDeliveryTest {

    /** 正式 reset link 永遠以 HTTPS 接收，與 SMTP 是否已加密是兩個不同的傳輸階段。 */
    @Test
    void allowsHttpsPublicBaseUrl() {
        assertTrue(SmtpPasswordResetDelivery.isAllowedResetBaseUrl("https://app.lumix.example", false));
    }

    /** 顯式開發開關只允許同一台機器的 loopback，不可變成任意 HTTP 的安全 bypass。 */
    @Test
    void onlyAllowsLoopbackHttpWhenExplicitlyEnabled() {
        assertTrue(SmtpPasswordResetDelivery.isAllowedResetBaseUrl("http://127.0.0.1:8088", true));
        assertTrue(SmtpPasswordResetDelivery.isAllowedResetBaseUrl("http://localhost:8088", true));
        assertFalse(SmtpPasswordResetDelivery.isAllowedResetBaseUrl("http://127.0.0.1:8088", false));
        assertFalse(SmtpPasswordResetDelivery.isAllowedResetBaseUrl("http://app.lumix.example", true));
    }

    @Test
    void superAdminRecoveryUsesDedicatedAdminResetRoute() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        UserAuthenticationProperties properties = new UserAuthenticationProperties();
        properties.getPasswordReset().setFromAddress("security@lumix.example");
        properties.getPasswordReset().setPublicBaseUrl("https://app.lumix.example");
        SmtpPasswordResetDelivery delivery = new SmtpPasswordResetDelivery(mailSender, properties, "smtp.lumix.example", true);

        delivery.deliverSuperAdminPasswordRecovery(
            new AuthenticatedUser("admin-1", "admin@lumix.example", "最高管理員"),
            new PasswordResetSecret(UUID.randomUUID(), "one-time-secret", "digest")
        );

        ArgumentCaptor<SimpleMailMessage> message = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(message.capture());
        // 高權限信件不可導回一般客戶端的 /reset-password，避免使用者誤入錯誤的身分流程。
        assertTrue(message.getValue().getText().contains("https://app.lumix.example/admin/reset-password?token=one-time-secret"));
    }
}
