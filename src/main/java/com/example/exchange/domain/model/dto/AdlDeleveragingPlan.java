/*
 * 檔案用途：領域 DTO，承載 ADL forced deleveraging plan。
 */
package com.example.exchange.domain.model.dto;

import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;


/**
 * ADL deleveraging plan。
 *
 * @param requestedNotional 需由 ADL 承接的缺口名義金額
 * @param plannedNotional   此計畫可覆蓋的名義金額
 * @param remainingNotional 候選不足時尚未覆蓋的名義金額
 * @param steps             依 ranking 產生的減倉步驟
 */
@Data
@Builder
@Jacksonized
public class AdlDeleveragingPlan {

    private final BigDecimal requestedNotional;

    private final BigDecimal plannedNotional;

    private final BigDecimal remainingNotional;

    private final List<AdlDeleveragingStep> steps;
    public AdlDeleveragingPlan(BigDecimal requestedNotional, BigDecimal plannedNotional, BigDecimal remainingNotional, List<AdlDeleveragingStep> steps) {
        this.requestedNotional = requestedNotional;
        this.plannedNotional = plannedNotional;
        this.remainingNotional = remainingNotional;
        this.steps = steps;
    }

    public BigDecimal requestedNotional() {
        return requestedNotional;
    }

    public BigDecimal plannedNotional() {
        return plannedNotional;
    }

    public BigDecimal remainingNotional() {
        return remainingNotional;
    }

    public List<AdlDeleveragingStep> steps() {
        return steps;
    }
}