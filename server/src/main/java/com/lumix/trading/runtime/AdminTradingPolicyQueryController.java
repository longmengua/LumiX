package com.lumix.trading.runtime;

import com.lumix.admin.superadmin.SuperAdminAccessService;
import com.lumix.infrastructure.security.ApiAuthenticationFilter;
import com.lumix.user.auth.domain.AuthenticatedUser;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 後台讀取完整目前政策與商品狀態；回傳的 JSON 可供明確建立下一版本。 */
@RestController
@Profile("infrastructure")
@RequestMapping("/api/admin/v1/trading/{kind}/policies/current")
public class AdminTradingPolicyQueryController {
    private final JdbcTemplate jdbc;
    private final SuperAdminAccessService access;

    public AdminTradingPolicyQueryController(JdbcTemplate jdbc,SuperAdminAccessService access){
        this.jdbc=jdbc;this.access=access;
    }

    /** JSON 原文保留十進位精度；管理頁不可經 JavaScript Number 往返政策金額。 */
    public record CurrentPolicy(String marketSymbol,String status,long policyVersion,String policy){}

    @GetMapping
    public ResponseEntity<CurrentPolicy> current(
            @RequestAttribute(ApiAuthenticationFilter.AUTHENTICATED_USER_ATTRIBUTE) AuthenticatedUser actor,
            @PathVariable String kind,@RequestParam String market) {
        access.requireActiveSuperAdmin(actor);
        String type=TradingPolicyRepository.kind(kind);
        if(market==null || !market.matches("[A-Z0-9][A-Z0-9_-]{0,31}"))
            throw new IllegalArgumentException("invalid trading market");
        String policyTable="futures".equals(kind)?"futures_risk_policies":"trading_execution_policies";
        List<java.util.Map<String,Object>> rows=jdbc.queryForList("SELECT i.status,i.policy_version,p.policy::text "
                + "AS policy FROM trading_instruments i JOIN "+policyTable+" p "
                + "ON p.instrument_id=i.instrument_id AND p.policy_version=i.policy_version "
                + "WHERE i.instrument_kind=? AND i.market_symbol=?",type,market);
        if(rows.isEmpty()) return ResponseEntity.notFound().build();
        var row=rows.getFirst();
        return ResponseEntity.ok().header("Cache-Control","no-store")
                .body(new CurrentPolicy(market,row.get("status").toString(),
                        ((Number)row.get("policy_version")).longValue(),
                        row.get("policy").toString()));
    }
}
