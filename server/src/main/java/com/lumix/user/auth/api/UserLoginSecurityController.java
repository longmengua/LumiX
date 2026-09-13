package com.lumix.user.auth.api;

import com.lumix.infrastructure.security.ApiAuthenticationFilter;
import com.lumix.user.auth.application.UserAuthenticationService;
import com.lumix.user.auth.application.UserAuthenticationService.LoginSecurityOverview;
import com.lumix.user.auth.domain.AuthenticatedUser;
import com.lumix.user.auth.domain.BoundLoginDevice;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 個人中心的受限綁定裝置 API。
 *
 * <p>所有權僅由 authentication filter 注入的 session principal 決定，所有 response 都排除 cookie、token、
 * digest 與完整 User-Agent，避免裝置清單反過來成為帳戶認證資料外洩來源。</p>
 */
@RestController
@Profile("infrastructure")
@RequestMapping("/api/v1/account/security")
public class UserLoginSecurityController {

    private final UserAuthenticationService authenticationService;

    public UserLoginSecurityController(UserAuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @GetMapping
    public LoginSecurityResponse getLoginSecurity(
        @RequestAttribute(ApiAuthenticationFilter.AUTHENTICATED_USER_ATTRIBUTE) AuthenticatedUser authenticatedUser
    ) {
        return LoginSecurityResponse.from(authenticationService.getLoginSecurityOverview(authenticatedUser));
    }

    /** 移除時會同步撤銷該裝置的 active sessions，不能只從畫面隱藏資料。 */
    @DeleteMapping("/devices/{deviceId}")
    public void removeBoundDevice(
        @RequestAttribute(ApiAuthenticationFilter.AUTHENTICATED_USER_ATTRIBUTE) AuthenticatedUser authenticatedUser,
        @PathVariable UUID deviceId
    ) {
        authenticationService.removeBoundLoginDevice(authenticatedUser, deviceId);
    }

    public record LoginSecurityResponse(List<BoundLoginDeviceResponse> devices, Instant fundTransferRestrictedUntil) {
        static LoginSecurityResponse from(LoginSecurityOverview overview) {
            return new LoginSecurityResponse(
                overview.devices().stream().map(BoundLoginDeviceResponse::from).toList(), overview.fundTransferRestrictedUntil()
            );
        }
    }

    public record BoundLoginDeviceResponse(
        UUID deviceId,
        String platform,
        String deviceLabel,
        String lastIpAddress,
        Instant createdAt,
        Instant lastSeenAt
    ) {
        static BoundLoginDeviceResponse from(BoundLoginDevice device) {
            return new BoundLoginDeviceResponse(
                device.deviceId(), device.platform().name(), device.deviceLabel(), device.lastIpAddress(), device.createdAt(), device.lastSeenAt()
            );
        }
    }
}
