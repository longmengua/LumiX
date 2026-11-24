package com.lumix.trading.liquidity;

import com.lumix.admin.superadmin.SuperAdminAccessService;
import com.lumix.api.error.ApiErrorCode;
import com.lumix.api.error.ApiException;
import com.lumix.infrastructure.security.ApiAuthenticationFilter;
import com.lumix.user.auth.domain.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** 最高管理員可讀寫資料庫 Demo 開關；此 API 不提供任何前端控制元件。 */
@RestController
@Profile("infrastructure")
@RequestMapping("/api/admin/v1/trading/external-order-proxy/settings")
public class ExternalOrderProxySettingsController {
    private final ExternalOrderProxySettingsService settings;
    private final SuperAdminAccessService access;
    private final String origin;

    public ExternalOrderProxySettingsController(ExternalOrderProxySettingsService settings,
            SuperAdminAccessService access,
            @Value("${lumix.trading.admin-allowed-origin:}") String origin) {
        this.settings = settings;
        this.access = access;
        this.origin = origin;
    }

    /** 讀取資料庫權威值；不從環境變數或前端狀態推定。 */
    @GetMapping
    public ResponseEntity<ExternalOrderProxySettingsService.Settings> current(
            @RequestAttribute(ApiAuthenticationFilter.AUTHENTICATED_USER_ATTRIBUTE) AuthenticatedUser actor) {
        access.requireActiveSuperAdmin(actor);
        return ResponseEntity.ok().header("Cache-Control", "no-store").body(settings.current());
    }

    /** 使用 expectedVersion 防止兩個管理命令互相覆蓋，成功時同交易寫入 audit log。 */
    @PutMapping(consumes = "application/json")
    public ResponseEntity<ExternalOrderProxySettingsService.Settings> update(
            @RequestAttribute(ApiAuthenticationFilter.AUTHENTICATED_USER_ATTRIBUTE) AuthenticatedUser actor,
            @RequestBody Command command, HttpServletRequest request) {
        if (origin.isBlank() || !origin.equals(request.getHeader("Origin"))
                || !"trading-policy".equals(request.getHeader("X-LumiX-Intent"))
                || "cross-site".equals(request.getHeader("Sec-Fetch-Site")))
            throw new ApiException(ApiErrorCode.AUTHORIZATION_ERROR);
        if (command == null || command.expectedVersion() < 1)
            throw new IllegalArgumentException("external proxy setting command is invalid");
        return ResponseEntity.ok().header("Cache-Control", "no-store")
                .body(settings.update(actor, command.demoEnabled(), command.expectedVersion()));
    }

    public record Command(boolean demoEnabled, long expectedVersion) { }
}
