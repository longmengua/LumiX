package com.lumix.trading.runtime;

import com.lumix.infrastructure.security.ApiAuthenticationFilter;
import com.lumix.user.auth.domain.AuthenticatedUser;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 客戶現貨／合約委託的 authenticated owner-only 查詢；不建立新單或暗示撮合已啟用。 */
@RestController
@Profile("infrastructure")
@RequestMapping("/api/v1/trading/{kind}/orders")
public class TradingOrderQueryController {
    private final TradingOrderQueryService orders;

    public TradingOrderQueryController(TradingOrderQueryService orders) { this.orders = orders; }

    /** owner 只取全域 session filter 的 principal，沒有 userId／accountId query parameter。 */
    @GetMapping
    public ResponseEntity<TradingOrderQueryService.Page> list(
            @RequestAttribute(ApiAuthenticationFilter.AUTHENTICATED_USER_ATTRIBUTE) AuthenticatedUser user,
            @PathVariable String kind, @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String before) {
        return ResponseEntity.ok().header("Cache-Control", "no-store").body(orders.list(user.userId(), kind, limit, before));
    }

    /** 他人委託與未知委託不區分，以免查單入口洩漏其他帳號交易活動。 */
    @GetMapping("/{orderId}")
    public ResponseEntity<TradingOrderQueryService.OrderView> detail(
            @RequestAttribute(ApiAuthenticationFilter.AUTHENTICATED_USER_ATTRIBUTE) AuthenticatedUser user,
            @PathVariable String kind, @PathVariable String orderId) {
        return ResponseEntity.ok().header("Cache-Control", "no-store").body(orders.detail(user.userId(), kind, orderId));
    }
}
