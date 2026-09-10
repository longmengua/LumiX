package com.lumix.user.auth.api;

import com.lumix.user.auth.application.UserAuthenticationService;
import com.lumix.user.auth.application.UserAuthenticationService.AuthenticationResult;
import com.lumix.user.auth.application.UserAuthenticationService.LoginResult;
import com.lumix.user.auth.application.UserAuthenticationService.LoginVerificationCompletion;
import com.lumix.user.auth.config.UserAuthenticationProperties;
import com.lumix.user.auth.domain.AuthenticatedUser;
import com.lumix.user.auth.domain.DeviceSecret;
import com.lumix.user.auth.domain.LoginVerificationState;
import com.lumix.user.auth.domain.PendingLoginVerificationSecret;
import com.lumix.user.auth.domain.SessionSecret;
import java.time.Duration;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 使用者登入 HTTP boundary。
 *
 * <p>前端永遠不讀取 session、device 或 pending verification secret：它們只會由 Set-Cookie 寫入
 * HttpOnly Cookie，並透過同源 `/api` 請求送回。系統尚未提供 CORS，避免瀏覽器認證 Cookie 被跨來源使用。</p>
 */
@RestController
@Profile("infrastructure")
@RequestMapping("/api/v1/auth")
public class UserAuthenticationController {

    private final UserAuthenticationService authenticationService;
    private final UserAuthenticationProperties properties;

    public UserAuthenticationController(
        UserAuthenticationService authenticationService,
        UserAuthenticationProperties properties
    ) {
        this.authenticationService = authenticationService;
        this.properties = properties;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(HttpServletRequest servletRequest, @RequestBody RegisterRequest request) {
        AuthenticationResult result = authenticationService.register(
            request.email(), request.displayName(), request.password(), LoginRequestMetadataResolver.resolve(servletRequest)
        );
        return authenticatedResponse(HttpStatus.CREATED, result);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(HttpServletRequest servletRequest, @RequestBody LoginRequest request) {
        LoginResult result = authenticationService.login(
            request.email(), request.password(), parseOptionalDevice(deviceCookieValue(servletRequest)),
            LoginRequestMetadataResolver.resolve(servletRequest)
        );
        if (result.requiresVerification()) {
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                .header(HttpHeaders.SET_COOKIE, pendingVerificationCookie(result.pendingVerification()).toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(new LoginVerificationPendingResponse(true));
        }
        return authenticatedResponse(HttpStatus.OK, result.authentication());
    }

    @GetMapping("/me")
    public UserResponse me(HttpServletRequest request) {
        return UserResponse.from(authenticationService.authenticate(parseSession(sessionCookieValue(request))));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        String cookieValue = sessionCookieValue(request);
        if (cookieValue != null) {
            try {
                authenticationService.logout(parseSession(cookieValue));
            } catch (RuntimeException ignored) {
                // 登出需保持可重試；不因舊 cookie 損壞而阻止瀏覽器清除它。
            }
        }
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, expiredSessionCookie().toString()).build();
    }

    @PostMapping("/password/change")
    public ResponseEntity<UserResponse> changePassword(
        HttpServletRequest servletRequest,
        @RequestBody ChangePasswordRequest request
    ) {
        AuthenticationResult result = authenticationService.changePassword(
            parseSession(sessionCookieValue(servletRequest)), request.currentPassword(), request.newPassword(),
            parseOptionalDevice(deviceCookieValue(servletRequest)), LoginRequestMetadataResolver.resolve(servletRequest)
        );
        return authenticatedResponse(HttpStatus.OK, result);
    }

    /**
     * email 確認頁的 Yes／No 決定。
     *
     * <p>這個 endpoint 不設 session cookie；核准後仍只能由原始登入瀏覽器的 pending cookie 完成登入。</p>
     */
    @PostMapping("/login-verification/decision")
    public LoginVerificationDecisionResponse decideLoginVerification(@RequestBody LoginVerificationDecisionRequest request) {
        LoginVerificationState state = authenticationService.decideLoginVerification(request.token(), request.approved());
        return new LoginVerificationDecisionResponse(state.name());
    }

    /** 原始登入瀏覽器以 HttpOnly pending cookie 輪詢並消耗已核准的新裝置登入。 */
    @PostMapping("/login-verification/complete")
    public ResponseEntity<?> completeLoginVerification(HttpServletRequest servletRequest) {
        LoginVerificationCompletion completion = authenticationService.completeLoginVerification(
            parsePendingVerification(pendingVerificationCookieValue(servletRequest)),
            LoginRequestMetadataResolver.resolve(servletRequest)
        );
        if (completion.state() == LoginVerificationState.PENDING) {
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(new LoginVerificationPendingResponse(true));
        }
        if (completion.state() == LoginVerificationState.REJECTED) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .header(HttpHeaders.SET_COOKIE, expiredPendingVerificationCookie().toString())
                .build();
        }
        return authenticatedResponse(HttpStatus.OK, completion.authentication(), true);
    }

    @PostMapping("/password/forgot")
    public ResponseEntity<Void> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        authenticationService.requestPasswordReset(request.email());
        // 不論帳號是否存在都採相同成功回應，避免由此 endpoint 枚舉 email。
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/password/reset")
    public ResponseEntity<Void> resetPassword(@RequestBody ResetPasswordRequest request) {
        authenticationService.resetPassword(request.token(), request.newPassword());
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<UserResponse> authenticatedResponse(HttpStatus status, AuthenticationResult result) {
        return authenticatedResponse(status, result, false);
    }

    private ResponseEntity<UserResponse> authenticatedResponse(
        HttpStatus status,
        AuthenticationResult result,
        boolean expirePendingVerification
    ) {
        ResponseEntity.BodyBuilder response = ResponseEntity.status(status)
            .header(HttpHeaders.SET_COOKIE, activeSessionCookie(result.session()).toString())
            .header(HttpHeaders.CACHE_CONTROL, "no-store");
        if (result.device() != null) {
            response.header(HttpHeaders.SET_COOKIE, activeDeviceCookie(result.device()).toString());
        }
        if (expirePendingVerification) {
            response.header(HttpHeaders.SET_COOKIE, expiredPendingVerificationCookie().toString());
        }
        return response.body(UserResponse.from(result.user()));
    }

    private SessionSecret parseSession(String cookieValue) {
        return UserAuthenticationService.parseCookieValue(cookieValue);
    }

    private String sessionCookieValue(HttpServletRequest request) {
        return cookieValue(request, properties.getCookieName());
    }

    private String deviceCookieValue(HttpServletRequest request) {
        return cookieValue(request, properties.getDeviceCookieName());
    }

    private String pendingVerificationCookieValue(HttpServletRequest request) {
        return cookieValue(request, properties.getLoginVerificationCookieName());
    }

    private static String cookieValue(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (cookieName.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private ResponseCookie activeSessionCookie(SessionSecret session) {
        return ResponseCookie.from(properties.getCookieName(), session.toCookieValue())
            .httpOnly(true)
            .secure(properties.isCookieSecure())
            // Strict 搭配同源 Nginx proxy，使跨站頁面無法攜帶認證 Cookie 發動狀態變更請求。
            .sameSite("Strict")
            .path("/api")
            .maxAge(properties.getSessionTtl())
            .build();
    }

    private ResponseCookie activeDeviceCookie(DeviceSecret device) {
        return ResponseCookie.from(properties.getDeviceCookieName(), device.toCookieValue())
            .httpOnly(true)
            .secure(properties.isCookieSecure())
            .sameSite("Strict")
            .path("/api")
            .maxAge(properties.getDeviceTtl())
            .build();
    }

    private ResponseCookie pendingVerificationCookie(PendingLoginVerificationSecret pending) {
        return ResponseCookie.from(properties.getLoginVerificationCookieName(), pending.toCookieValue())
            .httpOnly(true)
            .secure(properties.isCookieSecure())
            .sameSite("Strict")
            .path("/api")
            .maxAge(properties.getLoginVerification().getTtl())
            .build();
    }

    private ResponseCookie expiredSessionCookie() {
        return ResponseCookie.from(properties.getCookieName(), "")
            .httpOnly(true)
            .secure(properties.isCookieSecure())
            .sameSite("Strict")
            .path("/api")
            .maxAge(Duration.ZERO)
            .build();
    }

    private ResponseCookie expiredPendingVerificationCookie() {
        return ResponseCookie.from(properties.getLoginVerificationCookieName(), "")
            .httpOnly(true)
            .secure(properties.isCookieSecure())
            .sameSite("Strict")
            .path("/api")
            .maxAge(Duration.ZERO)
            .build();
    }

    private static DeviceSecret parseOptionalDevice(String cookieValue) {
        if (cookieValue == null) return null;
        try {
            return UserAuthenticationService.parseDeviceCookieValue(cookieValue);
        } catch (RuntimeException ignored) {
            // 壞掉的 device cookie 只能當作未知裝置，不能讓它變成繞過 email 驗證的理由。
            return null;
        }
    }

    private static PendingLoginVerificationSecret parsePendingVerification(String cookieValue) {
        return UserAuthenticationService.parsePendingLoginVerificationCookieValue(cookieValue);
    }

    /** 註冊輸入；password 不得加入 toString、log 或 validation error 的 details。 */
    public record RegisterRequest(String email, String displayName, String password) { }
    /** 登入輸入；錯誤回應不能區分 email 與 password 何者錯誤。 */
    public record LoginRequest(String email, String password) { }
    /** email 確認頁只接受一次性 token 與明確 Yes／No，不接受 userId、session 或裝置資料。 */
    public record LoginVerificationDecisionRequest(String token, boolean approved) { }
    /** 變更密碼必須持有有效 session 並重新驗證舊密碼。 */
    public record ChangePasswordRequest(String currentPassword, String newPassword) { }
    /** 忘記密碼一律採非枚舉回應。 */
    public record ForgotPasswordRequest(String email) { }
    /** 重設 token 只可由 HTTPS 信件連結攜入，使用後立即消耗。 */
    public record ResetPasswordRequest(String token, String newPassword) { }

    /** 原始登入頁只需知道是否等待 email，不取得 request id 或任何認證材料。 */
    public record LoginVerificationPendingResponse(boolean verificationRequired) { }
    /** 確認頁回傳目前決定，讓重複點選不能悄悄反轉既有 Yes／No。 */
    public record LoginVerificationDecisionResponse(String state) { }

    /** 對前端公開的安全使用者投影，刻意不包含狀態以外的內部認證資料。 */
    public record UserResponse(String userId, String email, String displayName) {
        static UserResponse from(AuthenticatedUser user) {
            return new UserResponse(user.userId(), user.email(), user.displayName());
        }
    }
}
