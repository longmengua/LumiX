package com.lumix.admin.superadmin;

import com.lumix.user.auth.application.UserAuthenticationService;
import com.lumix.user.auth.application.VisualCaptchaService;
import com.lumix.user.auth.application.VisualCaptchaService.CaptchaVerificationRequest;
import com.lumix.user.auth.application.SliderCaptchaService.CaptchaPurpose;
import com.lumix.user.auth.api.LoginRequestMetadataResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 最高管理員專用的密碼復原 HTTP 邊界。
 *
 * <p>此 controller 不建立管理員、不發行 session，也不回應帳號是否具權限；它只將 captcha 驗證後的請求
 * 交給 application service 以固定的後台連結寄送一次性 token。</p>
 */
@RestController
@Profile("infrastructure")
@RequestMapping("/api/admin/v1/auth/password")
public class SuperAdminPasswordRecoveryController {

    private final UserAuthenticationService authenticationService;
    private final VisualCaptchaService visualCaptchaService;

    public SuperAdminPasswordRecoveryController(
        UserAuthenticationService authenticationService,
        VisualCaptchaService visualCaptchaService
    ) {
        this.authenticationService = authenticationService;
        this.visualCaptchaService = visualCaptchaService;
    }

    /** 只有 CAPTCHA 驗證通過後才會受理，且不論帳戶是否為 ACTIVE admin 都回傳相同 accepted 結果。 */
    @PostMapping("/forgot")
    public ResponseEntity<Void> forgot(HttpServletRequest servletRequest, @RequestBody ForgotPasswordRequest request) {
        visualCaptchaService.consume(request.captchaToken(), CaptchaPurpose.PASSWORD_RESET, LoginRequestMetadataResolver.resolve(servletRequest));
        authenticationService.requestSuperAdminPasswordReset(request.email());
        return ResponseEntity.accepted().build();
    }

    /** 後台 token 只能重設仍屬 ACTIVE admin principal 的帳戶，並在成功時撤銷全部既有 session。 */
    @PostMapping("/reset")
    public ResponseEntity<Void> reset(@RequestBody ResetPasswordRequest request) {
        authenticationService.resetSuperAdminPassword(request.token(), request.newPassword());
        return ResponseEntity.noContent().build();
    }

    /** 忘記密碼請求不接受 userId，讓 server 能統一維持不洩漏帳戶存在性與權限狀態的回應。 */
    public record ForgotPasswordRequest(String email, String captchaToken) { }
    /** token 僅存在 email 連結與本次 request，controller 不記錄或回傳它。 */
    public record ResetPasswordRequest(String token, String newPassword) { }
}
