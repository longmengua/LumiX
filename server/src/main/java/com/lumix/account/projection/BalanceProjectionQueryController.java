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

/**
 * 使用者自己的資產餘額 projection API。
 *
 * <p>此 route 不接受 userId 或 accountId，避免把資料隔離責任交給 browser。所有回傳 amount 均為十進位字串，
 * 避免 JavaScript number 破壞 NUMERIC(36,18) 精度。</p>
 */
@RestController
@Profile("infrastructure")
@RequestMapping("/api/v1/assets/balances")
public class BalanceProjectionQueryController {

    private static final String SOURCE = "BALANCE_PROJECTION";
    private final BalanceProjectionQueryService service;

    public BalanceProjectionQueryController(BalanceProjectionQueryService service) {
        this.service = service;
    }

    /** 回傳目前登入者已有的 projection rows，以及每列的 projection／reconciliation evidence。 */
    @GetMapping
    public BalanceProjectionResponse list(
        @RequestAttribute(ApiAuthenticationFilter.AUTHENTICATED_USER_ATTRIBUTE) AuthenticatedUser authenticatedUser
    ) {
        List<AccountBalanceProjection> items = service.listFor(authenticatedUser);
        return new BalanceProjectionResponse(SOURCE, items.stream().map(BalanceProjectionItemResponse::from).toList());
    }

    /** source 明確是 read model，避免前端或營運誤稱這份 response 為 ledger source of truth。 */
    public record BalanceProjectionResponse(String source, List<BalanceProjectionItemResponse> items) { }

    /** amount 用 plain string 保持大額與小數精度；不能交由 IEEE-754 number 表示。 */
    public record BalanceProjectionItemResponse(
        String accountId,
        String accountType,
        String accountStatus,
        String assetSymbol,
        String assetDisplayName,
        String assetStatus,
        int precisionScale,
        String total,
        String available,
        String locked,
        long projectionVersion,
        Instant projectedAt,
        Instant reconciledAt,
        String freshness
    ) {
        static BalanceProjectionItemResponse from(AccountBalanceProjection projection) {
            return new BalanceProjectionItemResponse(
                projection.accountId().value(), projection.accountType().name(), projection.accountStatus().name(),
                projection.assetSymbol().value(), projection.assetDisplayName(), projection.assetStatus(), projection.precisionScale(),
                projection.total().value().toPlainString(), projection.available().value().toPlainString(),
                projection.locked().value().toPlainString(), projection.projectionVersion(), projection.projectedAt(),
                projection.reconciledAt(), projection.freshness().name()
            );
        }
    }
}
