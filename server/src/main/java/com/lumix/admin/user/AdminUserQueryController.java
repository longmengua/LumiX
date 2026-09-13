package com.lumix.admin.user;

import com.lumix.infrastructure.security.ApiAuthenticationFilter;
import com.lumix.user.auth.domain.AuthenticatedUser;
import java.time.Instant;
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

    /** 搜尋 UID、email 或顯示名稱，回傳去敏摘要且受 server-side limit 保護。 */
    @GetMapping
    public List<UserResponse> find(
        @RequestAttribute(ApiAuthenticationFilter.AUTHENTICATED_USER_ATTRIBUTE) AuthenticatedUser actor,
        @RequestParam(required = false) String q,
        @RequestParam(required = false) Integer limit
    ) {
        return service.find(actor, q, limit).stream().map(UserResponse::from).toList();
    }

    /** 取得單一使用者的去敏裝置摘要，不提供任何狀態變更命令。 */
    @GetMapping("/{userId}")
    public UserDetailResponse detail(
        @RequestAttribute(ApiAuthenticationFilter.AUTHENTICATED_USER_ATTRIBUTE) AuthenticatedUser actor,
        @PathVariable String userId
    ) {
        return UserDetailResponse.from(service.detail(actor, userId));
    }

    record UserResponse(
        String userId,
        String email,
        String displayName,
        String status,
        Instant createdAt,
        Instant lastLoginAt,
        Instant fundTransferRestrictedUntil
    ) {
        static UserResponse from(AdminUserSummary user) {
            return new UserResponse(
                user.userId(), user.email(), user.displayName(), user.status(), user.createdAt(), user.lastLoginAt(),
                user.fundTransferRestrictedUntil()
            );
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
