package com.lumix.user.auth.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 使用者認證 runtime 的受控設定。
 *
 * <p>敏感值一律由部署環境注入；本設定只保存行為開關與非秘密參數，避免把 SMTP 密碼或 token
 * 寫進 version control。</p>
 */
@ConfigurationProperties("lumix.auth")
public class UserAuthenticationProperties {

    private boolean cookieSecure;
    private String cookieName = "LUMIX_SESSION";
    private Duration sessionTtl = Duration.ofHours(8);
    private int bcryptStrength = 12;
    private PasswordReset passwordReset = new PasswordReset();

    public boolean isCookieSecure() { return cookieSecure; }
    public void setCookieSecure(boolean cookieSecure) { this.cookieSecure = cookieSecure; }
    public String getCookieName() { return cookieName; }
    public void setCookieName(String cookieName) { this.cookieName = cookieName; }
    public Duration getSessionTtl() { return sessionTtl; }
    public void setSessionTtl(Duration sessionTtl) { this.sessionTtl = sessionTtl; }
    public int getBcryptStrength() { return bcryptStrength; }
    public void setBcryptStrength(int bcryptStrength) { this.bcryptStrength = bcryptStrength; }
    public PasswordReset getPasswordReset() { return passwordReset; }
    public void setPasswordReset(PasswordReset passwordReset) { this.passwordReset = passwordReset; }

    /** 密碼重設寄送邊界的設定，SMTP 未啟用時必須 fail closed。 */
    public static class PasswordReset {
        private Duration ttl = Duration.ofMinutes(30);
        private boolean smtpEnabled;
        private boolean allowLoopbackHttp;
        private String fromAddress = "";
        private String publicBaseUrl = "";

        public Duration getTtl() { return ttl; }
        public void setTtl(Duration ttl) { this.ttl = ttl; }
        public boolean isSmtpEnabled() { return smtpEnabled; }
        public void setSmtpEnabled(boolean smtpEnabled) { this.smtpEnabled = smtpEnabled; }
        /** 僅限本機開發時允許 HTTP loopback reset link；任何一般 HTTP host 都不得放寬。 */
        public boolean isAllowLoopbackHttp() { return allowLoopbackHttp; }
        public void setAllowLoopbackHttp(boolean allowLoopbackHttp) { this.allowLoopbackHttp = allowLoopbackHttp; }
        public String getFromAddress() { return fromAddress; }
        public void setFromAddress(String fromAddress) { this.fromAddress = fromAddress; }
        public String getPublicBaseUrl() { return publicBaseUrl; }
        public void setPublicBaseUrl(String publicBaseUrl) { this.publicBaseUrl = publicBaseUrl; }
    }
}
