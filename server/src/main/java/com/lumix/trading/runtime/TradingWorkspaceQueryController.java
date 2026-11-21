package com.lumix.trading.runtime;

import com.lumix.infrastructure.security.ApiAuthenticationFilter;
import com.lumix.user.auth.domain.AuthenticatedUser;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** 現貨／永續共用唯讀畫面契約，owner 僅取 session principal。 */
@RestController
@Profile("infrastructure")
@RequestMapping("/api/v1/trading/{kind}/workspace")
public class TradingWorkspaceQueryController {
    private final TradingWorkspaceQueryService queries;
    public TradingWorkspaceQueryController(TradingWorkspaceQueryService queries){this.queries=queries;}
    /** 查詢仍需認證，不能因為含有聚合訂單簿而放寬資產與本人成交的授權。 */
    @GetMapping
    public ResponseEntity<TradingWorkspaceQueryService.Workspace> snapshot(
            @RequestAttribute(ApiAuthenticationFilter.AUTHENTICATED_USER_ATTRIBUTE) AuthenticatedUser user,
            @PathVariable String kind,@RequestParam String market) {
        return ResponseEntity.ok().header("Cache-Control","no-store").body(queries.snapshot(user.userId(),kind,market));
    }
}
