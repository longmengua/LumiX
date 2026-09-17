package com.lumix.admin.asset;

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
 * 資產沖銷流水的管理端唯讀對賬端點。
 *
 * <p>回應只包含已存在的管理操作、journal 與分錄交叉檢查，不會藉由讀取 API 修改餘額或補發資產。</p>
 */
@RestController
@Profile("infrastructure")
@RequestMapping("/api/admin/v1/assets/audit/adjustments")
public class AdminAssetAdjustmentAuditController {
    private final AdminAssetAdjustmentAuditService service;

    public AdminAssetAdjustmentAuditController(AdminAssetAdjustmentAuditService service) {
        this.service = service;
    }

    @GetMapping
    public Response list(@RequestAttribute(ApiAuthenticationFilter.AUTHENTICATED_USER_ATTRIBUTE) AuthenticatedUser actor) {
        return new Response(service.findLatest(actor).stream().map(Item::from).toList());
    }

    public record Response(List<Item> items) { }

    public record Item(
            String auditLogId,
            String ledgerJournalId,
            String actorId,
            String targetUserId,
            String targetUserEmail,
            String accountType,
            String assetSymbol,
            String direction,
            String amount,
            String activityType,
            String note,
            String reconciliationStatus,
            Instant postedAt
    ) {
        static Item from(AdminAssetAdjustmentAuditItem value) {
            return new Item(
                    Long.toString(value.auditLogId()),
                    Long.toString(value.ledgerJournalId()),
                    value.actorId(),
                    value.targetUserId(),
                    value.targetUserEmail(),
                    value.accountType().name(),
                    value.assetSymbol(),
                    value.direction(),
                    value.amount(),
                    value.activityType(),
                    value.note(),
                    value.reconciliationStatus(),
                    value.postedAt()
            );
        }
    }
}
