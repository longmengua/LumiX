package com.lumix.admin.superadmin;

import com.lumix.infrastructure.security.ApiAuthenticationFilter;
import com.lumix.user.auth.domain.AuthenticatedUser;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 後台前端載入時的最小身分確認 API。
 *
 * <p>這不是另一套登入或 token 發行機制；它只驗證既有 HttpOnly 使用者 session 是否對應已啟用的最高管理員，
 * 防止前端用 localStorage 模擬後台登入狀態。</p>
 */
@RestController
@Profile("infrastructure")
@RequestMapping("/api/admin/v1/session")
public class SuperAdminSessionController {

    private final SuperAdminAccessService accessService;

    public SuperAdminSessionController(SuperAdminAccessService accessService) {
        this.accessService = accessService;
    }

    /** 回傳顯示後台頁首所需的去敏主體，不回傳任何 credential 或權限寫入能力。 */
    @GetMapping
    public AdminSessionResponse current(
        @RequestAttribute(ApiAuthenticationFilter.AUTHENTICATED_USER_ATTRIBUTE) AuthenticatedUser actor
    ) {
        accessService.requireActiveSuperAdmin(actor);
        return new AdminSessionResponse(actor.userId(), actor.email(), actor.displayName());
    }

    record AdminSessionResponse(String userId, String email, String displayName) { }
}
