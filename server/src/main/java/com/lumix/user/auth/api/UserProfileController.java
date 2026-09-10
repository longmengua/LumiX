package com.lumix.user.auth.api;

import com.lumix.infrastructure.security.ApiAuthenticationFilter;
import com.lumix.user.auth.application.UserAuthenticationService;
import com.lumix.user.auth.domain.AuthenticatedUser;
import com.lumix.user.auth.domain.UserProfile;
import java.time.Instant;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 個人中心的基本 profile API。
 *
 * <p>所有資料主體都由全域 authentication filter 注入，端點不接受 userId，避免使用者以 URL 或 body
 * 指向其他帳號。此 controller 不處理 email、KYC、資產、權限或任何資金資料。</p>
 */
@RestController
@Profile("infrastructure")
@RequestMapping("/api/v1/account/profile")
public class UserProfileController {

    private final UserAuthenticationService authenticationService;

    public UserProfileController(UserAuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    /** 回傳目前登入者自己的去敏基本資料與註冊時間。 */
    @GetMapping
    public UserProfileResponse getProfile(
        @RequestAttribute(ApiAuthenticationFilter.AUTHENTICATED_USER_ATTRIBUTE) AuthenticatedUser authenticatedUser
    ) {
        return UserProfileResponse.from(authenticationService.getUserProfile(authenticatedUser));
    }

    /** 只允許更新顯示名稱；email 屬於登入識別資料，需由獨立驗證流程處理。 */
    @PatchMapping
    public UserResponse updateProfile(
        @RequestAttribute(ApiAuthenticationFilter.AUTHENTICATED_USER_ATTRIBUTE) AuthenticatedUser authenticatedUser,
        @RequestBody UpdateProfileRequest request
    ) {
        return UserResponse.from(authenticationService.updateDisplayName(authenticatedUser, request.displayName()));
    }

    /** 更新輸入不包含 userId 或 email，確保 owner 與登入識別資料不由此 route 變更。 */
    public record UpdateProfileRequest(String displayName) { }

    /** Profile 讀取 contract 不包含 credential、session、KYC 或資產欄位。 */
    public record UserProfileResponse(String userId, String email, String displayName, Instant createdAt) {
        static UserProfileResponse from(UserProfile profile) {
            return new UserProfileResponse(profile.userId(), profile.email(), profile.displayName(), profile.createdAt());
        }
    }

    /** 更新後回傳與登入狀態相容的最小使用者投影，供前端同步導覽列名稱。 */
    public record UserResponse(String userId, String email, String displayName) {
        static UserResponse from(AuthenticatedUser user) {
            return new UserResponse(user.userId(), user.email(), user.displayName());
        }
    }
}
