package com.lumix.account.runtime;

import com.lumix.infrastructure.security.ApiAuthenticationFilter;
import com.lumix.user.auth.domain.AuthenticatedUser;
import java.math.BigDecimal;
import java.util.Objects;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 平台使用者間的現貨資產轉出 API。
 *
 * <p>轉出者身分只取自已驗證 session；收款 UUID 僅用於解析已啟用的收款帳戶，browser 無法指定任一 account ID。</p>
 */
@RestController
@Profile("infrastructure")
@RequestMapping("/api/v1/assets/internal-transfers")
public class PlatformInternalTransferController {
    private final PlatformInternalTransferService service;

    public PlatformInternalTransferController(PlatformInternalTransferService service) {
        this.service = Objects.requireNonNull(service, "service must not be null");
    }

    @PostMapping
    public ResponseEntity<Response> transfer(
            @RequestAttribute(ApiAuthenticationFilter.AUTHENTICATED_USER_ATTRIBUTE) AuthenticatedUser actor,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody Request request) {
        PlatformInternalTransferService.PlatformInternalTransferResult result = service.transfer(
                actor, request.recipientUserId(), request.assetSymbol(), request.amount(), idempotencyKey);
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(new Response(Long.toString(result.ledgerJournalId()), result.replayed()));
    }

    public record Request(String recipientUserId, String assetSymbol, BigDecimal amount) { }
    public record Response(String ledgerJournalId, boolean replayed) { }
}
