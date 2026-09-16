package com.lumix.account.runtime;

import com.lumix.infrastructure.security.ApiAuthenticationFilter;
import com.lumix.user.auth.domain.AuthenticatedUser;
import java.math.BigDecimal;
import java.util.Objects;
import org.springframework.http.HttpHeaders;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 使用者帳戶劃轉 API；session principal 是唯一 owner 來源，不能信任 request body 的 user ID。 */
@RestController
@Profile("infrastructure")
@RequestMapping("/api/v1/assets/transfers")
public class InternalTransferController {
    private final InternalTransferService service;
    public InternalTransferController(InternalTransferService service) { this.service = Objects.requireNonNull(service, "service must not be null"); }
    @PostMapping
    public ResponseEntity<Response> transfer(@RequestAttribute(ApiAuthenticationFilter.AUTHENTICATED_USER_ATTRIBUTE) AuthenticatedUser actor,
                                             @RequestHeader("Idempotency-Key") String idempotencyKey, @RequestBody Request request) {
        InternalTransferService.InternalTransferResult result = service.transfer(actor, request.sourceAccountType(), request.destinationAccountType(), request.assetSymbol(), request.amount(), idempotencyKey);
        return ResponseEntity.ok().header(HttpHeaders.CACHE_CONTROL, "no-store").body(new Response(Long.toString(result.ledgerJournalId()), result.replayed()));
    }
    public record Request(String sourceAccountType, String destinationAccountType, String assetSymbol, BigDecimal amount) { }
    public record Response(String ledgerJournalId, boolean replayed) { }
}
