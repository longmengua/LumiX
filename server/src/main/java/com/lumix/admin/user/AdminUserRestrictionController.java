package com.lumix.admin.user;

import com.lumix.infrastructure.security.ApiAuthenticationFilter;
import com.lumix.user.auth.domain.AuthenticatedUser;
import java.time.Instant;
import java.util.Objects;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 管理端使用者限制 HTTP 邊界；實際授權與稽核均由 service 在 transaction 內執行。 */
@RestController
@Profile("infrastructure")
@RequestMapping("/api/admin/v1/users/{userId}/restrictions")
public class AdminUserRestrictionController {
    private final AdminUserRestrictionService service;

    public AdminUserRestrictionController(AdminUserRestrictionService service) {
        this.service = Objects.requireNonNull(service, "service must not be null");
    }

    /** PUT 使用目標狀態，因此安全重送不會重複撤銷 session 或重複寫入稽核。 */
    @PutMapping("/login")
    public ResponseEntity<Response> setLoginFrozen(
            @RequestAttribute(ApiAuthenticationFilter.AUTHENTICATED_USER_ATTRIBUTE) AuthenticatedUser actor,
            @PathVariable String userId,
            @RequestBody Request request
    ) {
        return response(service.setLoginFrozen(actor, userId, requiredFrozen(request)));
    }

    /** 提幣凍結會交由所有對他人／外部出金 runtime fail closed；不改動帳本或使用者餘額。 */
    @PutMapping("/withdrawal")
    public ResponseEntity<Response> setWithdrawalFrozen(
            @RequestAttribute(ApiAuthenticationFilter.AUTHENTICATED_USER_ATTRIBUTE) AuthenticatedUser actor,
            @PathVariable String userId,
            @RequestBody Request request
    ) {
        return response(service.setWithdrawalFrozen(actor, userId, requiredFrozen(request)));
    }

    private static ResponseEntity<Response> response(AdminUserRestrictionResult result) {
        return ResponseEntity.ok().header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(new Response(result.changed(), result.loginFrozen(), result.withdrawalFrozenAt()));
    }

    private static boolean requiredFrozen(Request request) {
        if (request == null || request.frozen() == null) {
            // 缺少目標狀態一律拒絕，避免空 body 被 Jackson 預設成解除凍結。
            throw new IllegalArgumentException("frozen must be specified");
        }
        return request.frozen();
    }

    public record Request(Boolean frozen) { }
    public record Response(boolean changed, boolean loginFrozen, Instant withdrawalFrozenAt) { }
}
