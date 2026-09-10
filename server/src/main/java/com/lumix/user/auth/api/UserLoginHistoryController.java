package com.lumix.user.auth.api;

import com.lumix.infrastructure.security.ApiAuthenticationFilter;
import com.lumix.user.auth.application.UserAuthenticationService;
import com.lumix.user.auth.domain.AuthenticatedUser;
import com.lumix.user.auth.domain.LoginHistoryEntry;
import com.lumix.user.auth.domain.LoginHistoryPage;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    /** 只回傳本人可辨識異常登入所需的去敏快照，完整 session 與認證材料維持在 server 邊界內。 */
    @GetMapping
    public LoginHistoryResponse getLoginHistory(
        @RequestAttribute(ApiAuthenticationFilter.AUTHENTICATED_USER_ATTRIBUTE) AuthenticatedUser authenticatedUser,
        @RequestParam(required = false) Integer limit,
        @RequestParam(required = false) String before,
        @RequestParam(required = false) String after,
        @RequestParam(required = false) String anchor
    ) {
        LoginHistoryPage page = authenticationService.getLoginHistory(
            authenticatedUser, limit, parseCursor(before), parseCursor(after), parseCursor(anchor)
        );
        List<LoginHistoryItemResponse> records = page.records().stream()
            .map(LoginHistoryItemResponse::from)
            .toList();
        return new LoginHistoryResponse(records, page.hasOlder(), page.hasNewer());
    }

    private static Instant parseCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) return null;
        try {
            return Instant.parse(cursor);
        } catch (DateTimeParseException exception) {
            throw new com.lumix.api.error.ApiException(com.lumix.api.error.ApiErrorCode.VALIDATION_ERROR);
        }
    }

    /** 外部 contract 不洩漏可重放的 session 資料，IP／裝置只屬於已認證的本人。 */
    public record LoginHistoryResponse(List<LoginHistoryItemResponse> records, boolean hasOlder, boolean hasNewer) { }

    public record LoginHistoryItemResponse(Instant occurredAt, String ipAddress, String deviceLabel) {
        static LoginHistoryItemResponse from(LoginHistoryEntry entry) {
            return new LoginHistoryItemResponse(entry.occurredAt(), entry.ipAddress(), entry.deviceLabel());
        }
    }
}
