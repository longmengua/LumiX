/*
 * 檔案用途：測試做市商 inventory-aware hedge strategy。
 */
package com.example.exchange.application.service;

import com.example.exchange.domain.model.dto.HedgeStrategyDecision;
import com.example.exchange.domain.model.dto.MarketMakerExposure;
import com.example.exchange.domain.model.dto.MarketMakerProfile;
import com.example.exchange.domain.model.dto.MarketMakerRiskLimit;
import com.example.exchange.domain.model.enums.OrderSide;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MarketMakerHedgeStrategyServiceTest {

    private final MarketMakerHedgeStrategyService service = new MarketMakerHedgeStrategyService();

    @Test
    @DisplayName("planReduceOnlyHedge 會對超過 long inventory limit 的 exposure 產生 sell hedge")
    void planReduceOnlyHedgeCreatesSellOrderForLongExcess() {
        MarketMakerProfile profile = profile(false);
        MarketMakerExposure exposure = exposure(new BigDecimal("2.000"), new BigDecimal("100.00"));

        // 流程：long notional=200，long limit=150，所以只需要 reduce-only sell 50 notional。
        HedgeStrategyDecision decision = service.planReduceOnlyHedge(profile, exposure, "inventory-ref-1");

        assertThat(decision.hedgeRequired()).isTrue();
        assertThat(decision.reason()).isEqualTo("REDUCE_ONLY_HEDGE_REQUIRED");
        assertThat(decision.orderRequest().side()).isEqualTo(OrderSide.SELL);
        assertThat(decision.orderRequest().quantity()).isEqualByComparingTo("0.500000000000000000");
        assertThat(decision.orderRequest().limitPrice()).isEqualByComparingTo("99.0000");
    }

    @Test
    @DisplayName("planReduceOnlyHedge 會對超過 short inventory limit 的 exposure 產生 buy hedge 並套用 order cap")
    void planReduceOnlyHedgeCreatesBuyOrderForShortExcessAndCapsOrderNotional() {
        MarketMakerProfile profile = profile(false);
        MarketMakerExposure exposure = exposure(new BigDecimal("-3.000"), new BigDecimal("100.00"));

        // 流程：short excess=180，但單筆 max order=80，因此先產生 80 notional 的 buy hedge。
        HedgeStrategyDecision decision = service.planReduceOnlyHedge(profile, exposure, "inventory-ref-2");

        assertThat(decision.hedgeRequired()).isTrue();
        assertThat(decision.orderRequest().side()).isEqualTo(OrderSide.BUY);
        assertThat(decision.orderRequest().quantity()).isEqualByComparingTo("0.800000000000000000");
        assertThat(decision.orderRequest().limitPrice()).isEqualByComparingTo("101.0000");
    }

    @Test
    @DisplayName("planReduceOnlyHedge 在 exposure 未超限或 kill switch 時不產生 hedge order")
    void planReduceOnlyHedgeSkipsWithinLimitOrKillSwitch() {
        MarketMakerExposure withinLimit = exposure(new BigDecimal("1.000"), new BigDecimal("100.00"));

        // 流程：未超限時不需要 hedge；kill switch 時也不能產生對外 venue order。
        HedgeStrategyDecision withinDecision = service.planReduceOnlyHedge(profile(false), withinLimit, "ref-1");
        HedgeStrategyDecision killDecision = service.planReduceOnlyHedge(profile(true), withinLimit, "ref-2");

        assertThat(withinDecision.hedgeRequired()).isFalse();
        assertThat(withinDecision.reason()).isEqualTo("WITHIN_INVENTORY_LIMIT");
        assertThat(withinDecision.orderRequest()).isNull();
        assertThat(killDecision.hedgeRequired()).isFalse();
        assertThat(killDecision.reason()).isEqualTo("KILL_SWITCH_ENABLED");
    }

    private static MarketMakerProfile profile(boolean killSwitch) {
        return new MarketMakerProfile(
                "mm-1",
                9001,
                true,
                List.of(new MarketMakerRiskLimit(
                        "BTCUSDT",
                        new BigDecimal("150.00"),
                        new BigDecimal("120.00"),
                        new BigDecimal("80.00"),
                        new BigDecimal("0.01"),
                        killSwitch
                ))
        );
    }

    private static MarketMakerExposure exposure(BigDecimal quantity, BigDecimal markPrice) {
        return new MarketMakerExposure(
                "mm-1",
                9001,
                "BTCUSDT",
                quantity,
                markPrice,
                quantity.multiply(markPrice)
        );
    }
}
