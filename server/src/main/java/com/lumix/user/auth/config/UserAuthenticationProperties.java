package com.lumix.user.auth.config;

import java.time.Duration;
import java.util.List;
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
    private Duration deviceChangeFundTransferRestriction = Duration.ofHours(24);
    private int bcryptStrength = 12;
    private PasswordReset passwordReset = new PasswordReset();
    private RegistrationVerification registrationVerification = new RegistrationVerification();
    private LoginVerification loginVerification = new LoginVerification();
    private Captcha captcha = new Captcha();

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
    /** 同類型裝置完成替換後，提款與帳戶間轉帳必須被後端拒絕的最短時間。 */
    public Duration getDeviceChangeFundTransferRestriction() { return deviceChangeFundTransferRestriction; }
    public void setDeviceChangeFundTransferRestriction(Duration deviceChangeFundTransferRestriction) {
        this.deviceChangeFundTransferRestriction = deviceChangeFundTransferRestriction;
    }
    public int getBcryptStrength() { return bcryptStrength; }
    public void setBcryptStrength(int bcryptStrength) { this.bcryptStrength = bcryptStrength; }
    public PasswordReset getPasswordReset() { return passwordReset; }
    public void setPasswordReset(PasswordReset passwordReset) { this.passwordReset = passwordReset; }
    public RegistrationVerification getRegistrationVerification() { return registrationVerification; }
    public void setRegistrationVerification(RegistrationVerification registrationVerification) { this.registrationVerification = registrationVerification; }
    public LoginVerification getLoginVerification() { return loginVerification; }
    public void setLoginVerification(LoginVerification loginVerification) { this.loginVerification = loginVerification; }
    public Captcha getCaptcha() { return captcha; }
    public void setCaptcha(Captcha captcha) { this.captcha = captcha; }

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

    /** 註冊 email 雙驗證碼的短時效與可審核嘗試上限；SMTP 沿用 password-reset 的受控設定。 */
    public static class RegistrationVerification {
        private Duration ttl = Duration.ofMinutes(10);
        private int maxAttempts = 5;
        private int letterOptionCount = 4;

        public Duration getTtl() { return ttl; }
        public void setTtl(Duration ttl) { this.ttl = ttl; }
        public int getMaxAttempts() { return maxAttempts; }
        public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }
        public int getLetterOptionCount() { return letterOptionCount; }
        public void setLetterOptionCount(int letterOptionCount) { this.letterOptionCount = letterOptionCount; }
    }

    /**
     * 圖形驗證題型選擇設定。
     *
     * <p>題型只能由 server 設定選出，browser 不可自行帶入偏好的類型；空白或未知設定會在 application
     * 層 fail-closed，而不是靜默退化成最容易的題目。</p>
     */
    public static class Captcha {
        private List<String> enabledTypes = List.of("SLIDER");
        private String selectionMode = "RANDOM";

        public List<String> getEnabledTypes() { return enabledTypes; }
        public void setEnabledTypes(List<String> enabledTypes) { this.enabledTypes = enabledTypes; }
        public String getSelectionMode() { return selectionMode; }
        public void setSelectionMode(String selectionMode) { this.selectionMode = selectionMode; }
    }
}
