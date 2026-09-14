package com.lumix.admin.user;

import com.lumix.infrastructure.security.ApiAuthenticationFilter;
import com.lumix.user.auth.domain.AuthenticatedUser;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** P27 使用者管理的唯讀 API；權限來自 server session 與已啟用最高管理員 principal。 */
@RestController
@Profile("infrastructure")
@RequestMapping("/api/admin/v1/users")
public class AdminUserQueryController {

    private final AdminUserQueryService service;

    public AdminUserQueryController(AdminUserQueryService service) {
        this.service = service;
    }

    /**
     * 以顯示名稱前綴與註冊時間區間搜尋去敏摘要。
     *
     * <p>createdBefore 與 lastLoginBefore 均為排他上界；游標由上一頁回應提供，不能以 offset 取代，避免高頁數查詢
     * 隨資料量線性變慢。</p>
     */
    @GetMapping
    public UserSearchResponse find(
        @RequestAttribute(ApiAuthenticationFilter.AUTHENTICATED_USER_ATTRIBUTE) AuthenticatedUser actor,
        @RequestParam(required = false) String displayNamePrefix,
        @RequestParam(required = false) String createdFrom,
        @RequestParam(required = false) String createdBefore,
        @RequestParam(required = false) String lastLoginFrom,
        @RequestParam(required = false) String lastLoginBefore,
        @RequestParam(required = false) String cursorCreatedAt,
        @RequestParam(required = false) String cursorUserId,
        @RequestParam(required = false) Integer limit
    ) {
        AdminUserSearchPage page = service.find(
            actor, displayNamePrefix, parseInstant(createdFrom), parseInstant(createdBefore),
            parseInstant(lastLoginFrom), parseInstant(lastLoginBefore),
            parseInstant(cursorCreatedAt), cursorUserId, limit
        );
        return new UserSearchResponse(
            page.items().stream().map(UserResponse::from).toList(),
            page.nextCursor() == null ? null : UserCursorResponse.from(page.nextCursor()),
            page.total(),
            page.pageSize()
        );
    }

    /** 取得單一使用者的去敏裝置摘要，不提供任何狀態變更命令。 */
    @GetMapping("/{userId}")
    public UserDetailResponse detail(
        @RequestAttribute(ApiAuthenticationFilter.AUTHENTICATED_USER_ATTRIBUTE) AuthenticatedUser actor,
        @PathVariable String userId
    ) {
        return UserDetailResponse.from(service.detail(actor, userId));
    }

    private static Instant parseInstant(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(rawValue);
        } catch (DateTimeParseException exception) {
            throw new com.lumix.api.error.ApiException(com.lumix.api.error.ApiErrorCode.VALIDATION_ERROR);
        }
    }

    record UserResponse(
        String userId,
        String email,
        String displayName,
        String status,
        Instant createdAt,
        Instant lastLoginAt,
        Instant fundTransferRestrictedUntil,
        boolean hasActiveRestriction
    ) {
        static UserResponse from(AdminUserSummary user) {
            return new UserResponse(
                user.userId(), user.email(), user.displayName(), user.status(), user.createdAt(), user.lastLoginAt(),
                user.fundTransferRestrictedUntil(), user.hasActiveRestriction()
            );
        }
    }

    /** total 與 pageSize 只描述目前篩選快照；翻頁位置仍必須使用 nextCursor。 */
    record UserSearchResponse(List<UserResponse> items, UserCursorResponse nextCursor, long total, int pageSize) { }

    record UserCursorResponse(Instant createdAt, String userId) {
        static UserCursorResponse from(AdminUserSearchCursor cursor) {
            return new UserCursorResponse(cursor.createdAt(), cursor.userId());
        }
    }

    record UserDetailResponse(UserResponse user, List<DeviceResponse> devices) {
        static UserDetailResponse from(AdminUserDetail detail) {
            return new UserDetailResponse(
                UserResponse.from(detail.user()),
                detail.devices().stream()
                    .map(device -> new DeviceResponse(device.platform(), device.label(), device.lastSeenAt()))
                    .toList()
            );
        }
    }

    record DeviceResponse(String platform, String label, Instant lastSeenAt) { }
}
