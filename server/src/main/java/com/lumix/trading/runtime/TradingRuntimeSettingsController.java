package com.lumix.trading.runtime;

import com.lumix.admin.superadmin.SuperAdminAccessService;
import com.lumix.api.error.ApiErrorCode;
import com.lumix.api.error.ApiException;
import com.lumix.infrastructure.security.ApiAuthenticationFilter;
import com.lumix.user.auth.domain.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 最高管理員讀寫資料庫交易總開關；此 API 不依賴前端狀態或環境變數。 */
@RestController
@Profile("infrastructure")
@RequestMapping("/api/admin/v1/trading/runtime-settings")
public class TradingRuntimeSettingsController {
    private final TradingRuntimeSettingsService settings;
    private final SuperAdminAccessService access;
    private final String origin;

    public TradingRuntimeSettingsController(TradingRuntimeSettingsService settings,
            SuperAdminAccessService access,
            @Value("${lumix.trading.admin-allowed-origin:}") String origin) {
        this.settings = settings;
        this.access = access;
        this.origin = origin;
    }

    @GetMapping
    public ResponseEntity<List<TradingRuntimeSettingsService.Setting>> current(
            @RequestAttribute(ApiAuthenticationFilter.AUTHENTICATED_USER_ATTRIBUTE) AuthenticatedUser actor) {
        access.requireActiveSuperAdmin(actor);
        return ResponseEntity.ok().header("Cache-Control", "no-store").body(settings.all());
    }

    @PutMapping(path = "/{kind}", consumes = "application/json")
    public ResponseEntity<TradingRuntimeSettingsService.Setting> update(
            @RequestAttribute(ApiAuthenticationFilter.AUTHENTICATED_USER_ATTRIBUTE) AuthenticatedUser actor,
            @PathVariable String kind, @RequestBody Command command, HttpServletRequest request) {
        if (origin.isBlank() || !origin.equals(request.getHeader("Origin"))
                || !"trading-policy".equals(request.getHeader("X-LumiX-Intent"))
                || "cross-site".equals(request.getHeader("Sec-Fetch-Site")))
            throw new ApiException(ApiErrorCode.AUTHORIZATION_ERROR);
        if (command == null || command.expectedVersion() < 1)
            throw new IllegalArgumentException("trading runtime setting command is invalid");
        return ResponseEntity.ok().header("Cache-Control", "no-store")
                .body(settings.update(actor, kind, command.enabled(), command.expectedVersion()));
    }

    public record Command(boolean enabled, long expectedVersion) { }
}
