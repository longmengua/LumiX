package com.lumix.admin.asset;

import com.lumix.infrastructure.security.ApiAuthenticationFilter;
import com.lumix.user.auth.domain.AuthenticatedUser;
import java.time.Instant;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 管理端指定使用者的資產 projection 查詢；不存在 projection 時回傳空集合，絕不製造零餘額。 */
@RestController
@Profile("infrastructure")
@RequestMapping("/api/admin/v1/users/{userId}/assets")
public class AdminUserAssetQueryController {
    private final AdminUserAssetQueryService service;
    public AdminUserAssetQueryController(AdminUserAssetQueryService service) { this.service = service; }
    @GetMapping public Response list(@RequestAttribute(ApiAuthenticationFilter.AUTHENTICATED_USER_ATTRIBUTE) AuthenticatedUser actor,
                                     @PathVariable String userId) {
        return new Response("BALANCE_PROJECTION", service.find(actor, userId).stream().map(Item::from).toList());
    }
    public record Response(String source, List<Item> items) { }
    public record Item(String accountType, String assetSymbol, String total, String available, String locked,
                       long projectionVersion, Instant projectedAt, Instant reconciledAt) {
        static Item from(AdminUserAssetProjection value) { return new Item(value.accountType().name(), value.assetSymbol().value(),
            value.total().value().toPlainString(), value.available().value().toPlainString(), value.locked().value().toPlainString(),
            value.projectionVersion(), value.projectedAt(), value.reconciledAt()); }
    }
    @GetMapping("/history") public List<HistoryItem> history(@RequestAttribute(ApiAuthenticationFilter.AUTHENTICATED_USER_ATTRIBUTE) AuthenticatedUser actor, @PathVariable String userId) {
        return service.history(actor, userId).stream().map(value -> new HistoryItem(Long.toString(value.entryId()), value.accountType().name(), value.assetSymbol().value(), value.direction(), value.amount().value().toPlainString(), value.referenceType(), value.referenceId(), value.postedAt())).toList();
    }
    public record HistoryItem(String entryId, String accountType, String assetSymbol, String direction, String amount, String referenceType, String referenceId, Instant postedAt) { }
    /** 帳戶容器與資產 projection 分開回傳，避免空帳戶被前端誤顯示為零餘額。 */
    @GetMapping("/accounts") public List<AccountItem> accounts(@RequestAttribute(ApiAuthenticationFilter.AUTHENTICATED_USER_ATTRIBUTE) AuthenticatedUser actor, @PathVariable String userId) {
        return service.accounts(actor, userId).stream().map(value -> new AccountItem(value.accountId().value(), value.accountType().name(), value.accountStatus().name(), value.createdAt())).toList();
    }
    public record AccountItem(String accountId, String accountType, String accountStatus, Instant createdAt) { }
}
