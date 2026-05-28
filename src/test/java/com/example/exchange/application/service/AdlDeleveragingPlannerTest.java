/*
 * 檔案用途：測試 ADL forced deleveraging plan 生成。
 */
package com.example.exchange.application.service;

import com.example.exchange.domain.model.dto.AdlDeleveragingPlan;
import com.example.exchange.domain.model.dto.AdlRankedPosition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ADL deleveraging planner tests。
 *
 * <p>固定 forced deleveraging plan 的切分方式，讓後續 executor 可以在同一排序與缺口下
 * 得到相同 reduce notional / reduce qty。</p>
 */
class AdlDeleveragingPlannerTest {

    private final AdlDeleveragingPlanner planner = new AdlDeleveragingPlanner();

    @Test
    @DisplayName("planner 會依 ranking 逐步分配 ADL 缺口並換算 reduce qty")
    /**
     * 流程：ADL 缺口大於第一名 notional -> 先吃滿第一名，再由第二名承接剩餘。
     */
    void planAllocatesShortfallByRanking() {
        AdlDeleveragingPlan plan = planner.plan(
                new BigDecimal("150"),
                List.of(
                        ranked(1, 10, "BTCUSDT", "100"),
                        ranked(2, 20, "BTCUSDT", "80"),
                        ranked(3, 30, "BTCUSDT", "200")
                ),
                Map.of("BTCUSDT", new BigDecimal("50"))
        );

        assertThat(plan.plannedNotional()).isEqualByComparingTo("150");
        assertThat(plan.remainingNotional()).isEqualByComparingTo("0");
        assertThat(plan.steps()).hasSize(2);
        assertThat(plan.steps()).extracting(step -> step.uid()).containsExactly(10L, 20L);
        assertThat(plan.steps()).extracting(step -> step.reduceNotional())
                .usingComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .containsExactly(new BigDecimal("100"), new BigDecimal("50"));
        assertThat(plan.steps()).extracting(step -> step.reduceQty())
                .usingComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .containsExactly(new BigDecimal("2.000000000000000000"), new BigDecimal("1.000000000000000000"));
    }

    @Test
    @DisplayName("planner 會回報候選不足時的 remaining notional")
    /**
     * 流程：ADL 缺口大於全部候選 notional -> planned 只能覆蓋可用量，remaining 保留未覆蓋缺口。
     */
    void planReportsRemainingWhenCandidatesAreInsufficient() {
        AdlDeleveragingPlan plan = planner.plan(
                new BigDecimal("300"),
                List.of(ranked(1, 10, "ETHUSDT", "120")),
                Map.of("ETHUSDT", new BigDecimal("60"))
        );

        assertThat(plan.plannedNotional()).isEqualByComparingTo("120");
        assertThat(plan.remainingNotional()).isEqualByComparingTo("180");
        assertThat(plan.steps()).singleElement()
                .extracting(step -> step.reduceQty())
                .isEqualTo(new BigDecimal("2.000000000000000000"));
    }

    /**
     * 建立 planner 測試用 ranked position，排序分數不影響 planner 分配，故使用固定值。
     */
    private static AdlRankedPosition ranked(int rank, long uid, String symbol, String notional) {
        return new AdlRankedPosition(
                rank,
                uid,
                symbol,
                BigDecimal.ONE,
                BigDecimal.TEN,
                new BigDecimal(notional)
        );
    }
}
