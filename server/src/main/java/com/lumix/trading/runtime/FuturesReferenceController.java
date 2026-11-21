package com.lumix.trading.runtime;

import com.lumix.marketfeed.binance.BinanceFuturesReferencePriceFeed;
import java.time.Instant;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 標記與指數價只回傳後端已驗證新鮮度的具名永續來源。 */
@RestController
@Profile("infrastructure")
@RequestMapping("/api/v1/trading/futures/reference")
public class FuturesReferenceController {
    private final FuturesRiskPolicyRepository policies;
    private final ObjectProvider<BinanceFuturesReferencePriceFeed> references;

    public FuturesReferenceController(FuturesRiskPolicyRepository policies,
            ObjectProvider<BinanceFuturesReferencePriceFeed> references){
        this.policies=policies;this.references=references;
    }

    public record Reference(String source,String markPrice,String indexPrice,Instant sourceAt){}

    @GetMapping
    public ResponseEntity<Reference> current(@RequestParam String market){
        if (market==null || !market.matches("[A-Z0-9][A-Z0-9_-]{0,31}"))
            throw new IllegalArgumentException("invalid futures market");
        var policy=policies.active(market);
        var rules=policy.execution().rules();
        var feed=references.getIfAvailable();
        var snapshot=feed==null?null:feed.current(rules.baseAsset()+rules.quoteAsset(),
                policy.maximumPriceAge(),Instant.now()).orElse(null);
        if (snapshot==null || !snapshot.source().equals(policy.markPriceSource())
                || !snapshot.source().equals(policy.indexPriceSource()))
            throw new TradingOrderRejectedException("fresh futures reference is unavailable");
        return ResponseEntity.ok().header("Cache-Control","no-store").body(new Reference(snapshot.source(),
                snapshot.markPrice().stripTrailingZeros().toPlainString(),
                snapshot.indexPrice().stripTrailingZeros().toPlainString(),snapshot.sourceAt()));
    }
}
