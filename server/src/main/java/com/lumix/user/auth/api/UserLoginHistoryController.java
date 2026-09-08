package com.lumix.user.auth.api;

import com.lumix.infrastructure.security.ApiAuthenticationFilter;
import com.lumix.user.auth.application.UserAuthenticationService;
import com.lumix.user.auth.domain.AuthenticatedUser;
import com.lumix.user.auth.domain.LoginHistoryEntry;
import java.time.Instant;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 目前登入使用者的成功登入歷程 API。
 *
 * <p>端點刻意不接收 userId；全域 authentication filter 已驗證 HttpOnly session 並注入 principal，
 * 因此使用者不能透過修改 path 或 query 讀取其他帳號的安全資料。</p>
 */
@RestController
@Profile("infrastructure")
@RequestMapping("/api/v1/account/login-history")
public class UserLoginHistoryController {

    private final UserAuthenticationService authenticationService;

    public UserLoginHistoryController(UserAuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    /** 只回傳去敏的登入時間，完整 session 與認證材料維持在 server 邊界內。 */
    @GetMapping
    public LoginHistoryResponse getLoginHistory(
        @RequestAttribute(ApiAuthenticationFilter.AUTHENTICATED_USER_ATTRIBUTE) AuthenticatedUser authenticatedUser
    ) {
        List<LoginHistoryItemResponse> records = authenticationService.getLoginHistory(authenticatedUser).stream()
            .map(LoginHistoryItemResponse::from)
            .toList();
        return new LoginHistoryResponse(records);
    }

    /** 外部 contract 只含成功登入時間，不洩漏可關聯認證 session 的內部欄位。 */
    public record LoginHistoryResponse(List<LoginHistoryItemResponse> records) { }

    public record LoginHistoryItemResponse(Instant occurredAt) {
        static LoginHistoryItemResponse from(LoginHistoryEntry entry) {
            return new LoginHistoryItemResponse(entry.occurredAt());
        }
    }
}
