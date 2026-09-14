package com.lumix.admin.superadmin;

import com.lumix.user.auth.api.LoginRequestMetadataResolver;
import com.lumix.user.auth.application.SliderCaptchaService.CaptchaPurpose;
import com.lumix.user.auth.application.SliderCaptchaService.CaptchaVerificationResponse;
import com.lumix.user.auth.application.UserAuthenticationService;
import com.lumix.user.auth.application.UserAuthenticationService.AuthenticationResult;
import com.lumix.user.auth.application.VisualCaptchaService;
import com.lumix.user.auth.application.VisualCaptchaService.CaptchaChallengeResponse;
import com.lumix.user.auth.application.VisualCaptchaService.CaptchaVerificationRequest;
import com.lumix.user.auth.config.UserAuthenticationProperties;
import com.lumix.user.auth.domain.SessionSecret;
import com.lumix.user.auth.domain.AuthenticatedUser;
import com.lumix.infrastructure.security.ApiAuthenticationFilter;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 最高管理員專用登入 HTTP boundary。
 *
 * <p>管理端只接受 email、password 與一次性 CAPTCHA token；它不委派到一般登入端點，避免一般帳戶的
 * device verification 與 cookie 契約混入後台。session 仍由 server 以 HttpOnly cookie 保存，前端永遠
 * 不讀取 token。</p>
 */
@RestController
@Profile("infrastructure")
@RequestMapping("/api/admin/v1/auth")
public class SuperAdminAuthenticationController {

    private final UserAuthenticationService authenticationService;
    private final VisualCaptchaService visualCaptchaService;
    private final UserAuthenticationProperties properties;
    private final SuperAdminAccessService accessService;

    public SuperAdminAuthenticationController(
        UserAuthenticationService authenticationService,
        VisualCaptchaService visualCaptchaService,
        UserAuthenticationProperties properties,
        SuperAdminAccessService accessService
    ) {
        this.authenticationService = authenticationService;
        this.visualCaptchaService = visualCaptchaService;
        this.properties = properties;
        this.accessService = accessService;
    }

    /** CAPTCHA 先消耗再查驗帳密與 ACTIVE principal，避免管理登入成為可大量嘗試的例外入口。 */
    @PostMapping("/login")
    public ResponseEntity<AdminUserResponse> login(HttpServletRequest servletRequest, @RequestBody LoginRequest request) {
        visualCaptchaService.consume(request.captchaToken(), CaptchaPurpose.LOGIN, LoginRequestMetadataResolver.resolve(servletRequest));
        AuthenticationResult result = authenticationService.loginActiveSuperAdmin(
            request.email(), request.password(), LoginRequestMetadataResolver.resolve(servletRequest)
        );
        return ResponseEntity.status(HttpStatus.OK)
            .header(HttpHeaders.CACHE_CONTROL, "no-store")
            .header(HttpHeaders.SET_COOKIE, activeAdminSessionCookie(result.session()).toString())
            .body(AdminUserResponse.from(result));
    }

    /** 管理端題目由專用 URL 建立，答案仍只保存在 server-side CAPTCHA store。 */
    @GetMapping("/captcha/challenge")
    public CaptchaChallengeResponse createCaptchaChallenge(HttpServletRequest servletRequest) {
        return visualCaptchaService.create(LoginRequestMetadataResolver.resolve(servletRequest));
    }

    /** 驗證成功只核發短時效、用途限定 token；它不是 session，也不可存進 browser storage。 */
    @PostMapping("/captcha/challenge/verify")
    public CaptchaVerificationResponse verifyCaptchaChallenge(
        HttpServletRequest servletRequest,
        @RequestBody CaptchaVerificationRequest request
    ) {
        return visualCaptchaService.verify(request, LoginRequestMetadataResolver.resolve(servletRequest));
    }

    /** 登出只撤銷管理 session，絕不轉呼叫前台 auth endpoint 或清除前台 cookie。 */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
        HttpServletRequest servletRequest,
        @RequestAttribute(ApiAuthenticationFilter.AUTHENTICATED_USER_ATTRIBUTE) AuthenticatedUser actor
    ) {
        accessService.requireActiveSuperAdmin(actor);
        authenticationService.logout(UserAuthenticationService.parseCookieValue(adminSessionCookieValue(servletRequest)));
        return ResponseEntity.noContent()
            .header(HttpHeaders.SET_COOKIE, expiredAdminSessionCookie().toString())
            .header(HttpHeaders.CACHE_CONTROL, "no-store")
            .build();
    }

    /** 管理 session 僅附帶 admin API path，避免和一般 `/api` session 相同名稱或 scope。 */
    private ResponseCookie activeAdminSessionCookie(SessionSecret session) {
        return ResponseCookie.from(properties.getAdminCookieName(), session.toCookieValue())
            .httpOnly(true)
            .secure(properties.isCookieSecure())
            .sameSite("Strict")
            .path("/api/admin")
            .maxAge(properties.getSessionTtl())
            .build();
    }

    private ResponseCookie expiredAdminSessionCookie() {
        return ResponseCookie.from(properties.getAdminCookieName(), "")
            .httpOnly(true)
            .secure(properties.isCookieSecure())
            .sameSite("Strict")
            .path("/api/admin")
            .maxAge(Duration.ZERO)
            .build();
    }

    private String adminSessionCookieValue(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (properties.getAdminCookieName().equals(cookie.getName())) return cookie.getValue();
            }
        }
        // 全域 filter 正常情況已先驗證 cookie；缺失時仍以相同 authentication 語意拒絕。
        throw new com.lumix.api.error.ApiException(com.lumix.api.error.ApiErrorCode.AUTHENTICATION_ERROR);
    }

    /** password 不可被 controller log、例外 detail 或 response 回傳。 */
    public record LoginRequest(String email, String password, String captchaToken) { }
    /** 只回傳登入後 UI 必需的去敏 principal，不能回傳 session 或角色內部資料。 */
    public record AdminUserResponse(String userId, String email, String displayName) {
        private static AdminUserResponse from(AuthenticationResult result) {
            return new AdminUserResponse(result.user().userId(), result.user().email(), result.user().displayName());
        }
    }
}
