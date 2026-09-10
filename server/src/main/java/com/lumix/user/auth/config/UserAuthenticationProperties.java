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
    private String deviceCookieName = "LUMIX_DEVICE";
    private String loginVerificationCookieName = "LUMIX_LOGIN_VERIFICATION";
    private Duration sessionTtl = Duration.ofHours(8);
    private Duration deviceTtl = Duration.ofDays(90);
    private int bcryptStrength = 12;
    private PasswordReset passwordReset = new PasswordReset();
    private LoginVerification loginVerification = new LoginVerification();

    public boolean isCookieSecure() { return cookieSecure; }
    public void setCookieSecure(boolean cookieSecure) { this.cookieSecure = cookieSecure; }
    public String getCookieName() { return cookieName; }
    public void setCookieName(String cookieName) { this.cookieName = cookieName; }
    public String getDeviceCookieName() { return deviceCookieName; }
    public void setDeviceCookieName(String deviceCookieName) { this.deviceCookieName = deviceCookieName; }
    public String getLoginVerificationCookieName() { return loginVerificationCookieName; }
    public void setLoginVerificationCookieName(String loginVerificationCookieName) { this.loginVerificationCookieName = loginVerificationCookieName; }
    public Duration getSessionTtl() { return sessionTtl; }
    public void setSessionTtl(Duration sessionTtl) { this.sessionTtl = sessionTtl; }
    public Duration getDeviceTtl() { return deviceTtl; }
    public void setDeviceTtl(Duration deviceTtl) { this.deviceTtl = deviceTtl; }
    public int getBcryptStrength() { return bcryptStrength; }
    public void setBcryptStrength(int bcryptStrength) { this.bcryptStrength = bcryptStrength; }
    public PasswordReset getPasswordReset() { return passwordReset; }
    public void setPasswordReset(PasswordReset passwordReset) { this.passwordReset = passwordReset; }
    public LoginVerification getLoginVerification() { return loginVerification; }
    public void setLoginVerification(LoginVerification loginVerification) { this.loginVerification = loginVerification; }

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

    /** 新裝置登入確認的短時效設定；SMTP 與公開網址刻意沿用 password-reset 的受控邊界。 */
    public static class LoginVerification {
        private Duration ttl = Duration.ofMinutes(15);

        public Duration getTtl() { return ttl; }
        public void setTtl(Duration ttl) { this.ttl = ttl; }
    }
}
