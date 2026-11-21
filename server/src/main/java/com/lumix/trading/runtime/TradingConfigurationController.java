package com.lumix.trading.runtime;

import java.util.NoSuchElementException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** 下單能力由後端完整政策與啟用設定決定；畫面不得靠路徑或假餘額自行開啟。 */
@RestController
@Profile("infrastructure")
@RequestMapping("/api/v1/trading/{kind}/configuration")
public class TradingConfigurationController {
    private final TradingPolicyRepository policies;
    private final boolean enabled;
    public TradingConfigurationController(TradingPolicyRepository policies,@Value("${lumix.trading.spot-enabled:false}") boolean enabled){this.policies=policies;this.enabled=enabled;}
    /** 未完成永續風險閉環時明確回不可開倉，不讓現貨開關間接啟用合約。 */
    @GetMapping
    public ResponseEntity<Configuration> configuration(@PathVariable String kind,@RequestParam String market) {
        TradingPolicyRepository.kind(kind);
        if(market==null||!market.matches("[A-Z0-9][A-Z0-9_-]{0,31}"))throw new IllegalArgumentException("invalid market");
        Configuration value;
        if("futures".equals(kind))value=new Configuration(false,"FUTURES_RISK_RUNTIME_UNAVAILABLE",null,null,null,null,null,null,null,null);
        else {
            try {
                var p=policies.active(kind,market);var r=p.rules();
                value=new Configuration(enabled,enabled?"AVAILABLE":"NEW_ORDERS_DISABLED",r.baseAsset(),r.quoteAsset(),text(r.priceTick()),text(r.quantityStep()),text(r.minimumQuantity()),text(r.minimumNotional()),text(r.makerFeeRate()),text(r.takerFeeRate()));
            }catch(NoSuchElementException missing){value=new Configuration(false,"POLICY_UNAVAILABLE",null,null,null,null,null,null,null,null);}
        }
        return ResponseEntity.ok().header("Cache-Control","no-store").body(value);
    }
    private static String text(java.math.BigDecimal value){return value.stripTrailingZeros().toPlainString();}
    /** 費率與最小單位均以 decimal string 傳遞，實際成交費用仍由後端 authoritative 結算。 */
    public record Configuration(boolean ordersEnabled,String reason,String baseAsset,String quoteAsset,String priceTick,String quantityStep,String minimumQuantity,String minimumNotional,String makerFeeRate,String takerFeeRate){}
}
