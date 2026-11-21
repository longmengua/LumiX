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
    private final FuturesRiskPolicyRepository futures;
    private final boolean spotEnabled;
    private final boolean futuresEnabled;
    public TradingConfigurationController(TradingPolicyRepository policies, FuturesRiskPolicyRepository futures,
            @Value("${lumix.trading.spot-enabled:false}") boolean spotEnabled,
            @Value("${lumix.trading.futures-enabled:false}") boolean futuresEnabled){
        this.policies=policies;this.futures=futures;this.spotEnabled=spotEnabled;this.futuresEnabled=futuresEnabled;
    }
    @GetMapping
    public ResponseEntity<Configuration> configuration(@PathVariable String kind,@RequestParam String market) {
        TradingPolicyRepository.kind(kind);
        if(market==null||!market.matches("[A-Z0-9][A-Z0-9_-]{0,31}"))throw new IllegalArgumentException("invalid market");
        Configuration value;
        if("futures".equals(kind)) {
            try {
                var live=futures.tradable(market);var p=live.policy();var r=p.execution().rules();
                boolean available=futuresEnabled && "USDT".equals(p.collateralAsset());
                value=new Configuration(available,available?("ACTIVE".equals(live.status())?"AVAILABLE":"REDUCE_ONLY"):"NEW_ORDERS_DISABLED",r.baseAsset(),r.quoteAsset(),text(r.priceTick()),text(r.quantityStep()),text(r.minimumQuantity()),text(r.minimumNotional()),text(r.makerFeeRate()),text(r.takerFeeRate()));
            }catch(NoSuchElementException missing){value=new Configuration(false,"POLICY_UNAVAILABLE",null,null,null,null,null,null,null,null);}
        }
        else {
            try {
                var p=policies.active(kind,market);var r=p.rules();
                value=new Configuration(spotEnabled,spotEnabled?"AVAILABLE":"NEW_ORDERS_DISABLED",r.baseAsset(),r.quoteAsset(),text(r.priceTick()),text(r.quantityStep()),text(r.minimumQuantity()),text(r.minimumNotional()),text(r.makerFeeRate()),text(r.takerFeeRate()));
            }catch(NoSuchElementException missing){value=new Configuration(false,"POLICY_UNAVAILABLE",null,null,null,null,null,null,null,null);}
        }
        return ResponseEntity.ok().header("Cache-Control","no-store").body(value);
    }
    private static String text(java.math.BigDecimal value){return value.stripTrailingZeros().toPlainString();}
    /** 費率與最小單位均以 decimal string 傳遞，實際成交費用仍由後端 authoritative 結算。 */
    public record Configuration(boolean ordersEnabled,String reason,String baseAsset,String quoteAsset,String priceTick,String quantityStep,String minimumQuantity,String minimumNotional,String makerFeeRate,String takerFeeRate){}
}
