package com.lumix.admin.superadmin;

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
 * 管理員本人登入紀錄的只讀 API。
 *
 * <p>它不接受 userId，並在讀取前再次確認 ACTIVE principal；回傳資料僅限本人的登入時間、去敏裝置標籤與
 * IP，不能回傳 session secret 或裝置綁定資料。</p>
 */
@RestController
@Profile("infrastructure")
@RequestMapping("/api/admin/v1/account/login-history")
public class SuperAdminLoginHistoryController {

    private final UserAuthenticationService authenticationService;
    private final SuperAdminAccessService accessService;

    public SuperAdminLoginHistoryController(
        UserAuthenticationService authenticationService,
        SuperAdminAccessService accessService
    ) {
        this.authenticationService = authenticationService;
        this.accessService = accessService;
    }

    @GetMapping
    public LoginHistoryResponse getLoginHistory(
        @RequestAttribute(ApiAuthenticationFilter.AUTHENTICATED_USER_ATTRIBUTE) AuthenticatedUser actor,
        @RequestParam(required = false) Integer limit,
        @RequestParam(required = false) String before,
        @RequestParam(required = false) String after,
        @RequestParam(required = false) String anchor
    ) {
        accessService.requireActiveSuperAdmin(actor);
        LoginHistoryPage page = authenticationService.getLoginHistory(
            actor, limit, parseCursor(before), parseCursor(after), parseCursor(anchor)
        );
        return new LoginHistoryResponse(page.records().stream().map(LoginHistoryItemResponse::from).toList(), page.hasOlder(), page.hasNewer());
    }

    private static Instant parseCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) return null;
        try {
            return Instant.parse(cursor);
        } catch (DateTimeParseException exception) {
            throw new com.lumix.api.error.ApiException(com.lumix.api.error.ApiErrorCode.VALIDATION_ERROR);
        }
    }

    public record LoginHistoryResponse(List<LoginHistoryItemResponse> records, boolean hasOlder, boolean hasNewer) { }
    public record LoginHistoryItemResponse(Instant occurredAt, String ipAddress, String deviceLabel) {
        static LoginHistoryItemResponse from(LoginHistoryEntry entry) {
            return new LoginHistoryItemResponse(entry.occurredAt(), entry.ipAddress(), entry.deviceLabel());
        }
    }
}
