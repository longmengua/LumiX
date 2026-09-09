package com.lumix.user.auth.mail;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

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
}
