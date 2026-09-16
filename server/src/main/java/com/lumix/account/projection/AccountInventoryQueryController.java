package com.lumix.account.projection;

import com.lumix.infrastructure.security.ApiAuthenticationFilter;
import com.lumix.user.auth.domain.AuthenticatedUser;
import java.time.Instant;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 使用者帳戶容器 inventory API；source 明確標示不是餘額或帳本。 */
@RestController @Profile("infrastructure") @RequestMapping("/api/v1/assets/accounts")
public class AccountInventoryQueryController {
    private final AccountInventoryQueryService service;
    public AccountInventoryQueryController(AccountInventoryQueryService service) { this.service = service; }
    @GetMapping public Response list(@RequestAttribute(ApiAuthenticationFilter.AUTHENTICATED_USER_ATTRIBUTE) AuthenticatedUser owner) {
        return new Response("ACCOUNT_INVENTORY", service.find(owner).stream().map(Item::from).toList());
    }
    public record Response(String source, List<Item> items) { }
    public record Item(String accountId, String accountType, String accountStatus, Instant createdAt) {
        static Item from(AccountInventoryItem value) { return new Item(value.accountId().value(), value.accountType().name(), value.accountStatus().name(), value.createdAt()); }
    }
}
