/*
 * 檔案用途：應用服務，依 ADL ranking 產生 forced deleveraging plan。
 */
package com.example.exchange.application.service;

import com.example.exchange.domain.model.dto.AdlDeleveragingPlan;
import com.example.exchange.domain.model.dto.AdlDeleveragingStep;
import com.example.exchange.domain.model.dto.AdlRankedPosition;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * ADL forced deleveraging planner。
 *
 * <p>Planner 只產生 deterministic execution plan，不直接改倉位或帳務。Production executor
 * 可用此 plan 對 ranked counterparty 逐步執行 forced reduce。</p>
 */
@Service
public class AdlDeleveragingPlanner {

    private static final int SCALE = 18;

    /**
     * 依 ranked positions 從高優先級開始分配 ADL 缺口。
     *
     * @param requestedNotional ADL 需承接的缺口名義金額
     * @param rankedPositions   已排序 ADL 候選
     * @param markPrices        symbol -> mark price，用於把 notional 轉成 reduce qty
     */
    public AdlDeleveragingPlan plan(
            BigDecimal requestedNotional,
            List<AdlRankedPosition> rankedPositions,
            Map<String, BigDecimal> markPrices
    ) {
        BigDecimal remaining = requestedNotional == null ? BigDecimal.ZERO : requestedNotional.max(BigDecimal.ZERO);
        if (remaining.signum() == 0 || rankedPositions == null || rankedPositions.isEmpty()) {
            return new AdlDeleveragingPlan(remaining, BigDecimal.ZERO, remaining, List.of());
        }

        List<AdlDeleveragingStep> steps = new ArrayList<>();
        BigDecimal planned = BigDecimal.ZERO;
        for (AdlRankedPosition position : rankedPositions) {
            if (remaining.signum() <= 0) break;
            BigDecimal markPrice = markPrices == null ? null : markPrices.get(position.symbol());
            if (markPrice == null || markPrice.signum() <= 0 || position.notional().signum() <= 0) continue;
            BigDecimal reduceNotional = position.notional().min(remaining);
            BigDecimal reduceQty = reduceNotional.divide(markPrice, SCALE, RoundingMode.HALF_UP);
            steps.add(new AdlDeleveragingStep(
                    position.rank(),
                    position.uid(),
                    position.symbol(),
                    reduceNotional,
                    reduceQty
            ));
            planned = planned.add(reduceNotional);
            remaining = remaining.subtract(reduceNotional);
        }
        return new AdlDeleveragingPlan(
                requestedNotional.max(BigDecimal.ZERO),
                planned,
                remaining,
                List.copyOf(steps)
        );
    }
}
