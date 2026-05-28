/*
 * 檔案用途：領域 DTO，承載 ADL forced deleveraging plan。
 */
package com.example.exchange.domain.model.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * ADL deleveraging plan。
 *
 * @param requestedNotional 需由 ADL 承接的缺口名義金額
 * @param plannedNotional   此計畫可覆蓋的名義金額
 * @param remainingNotional 候選不足時尚未覆蓋的名義金額
 * @param steps             依 ranking 產生的減倉步驟
 */
public record AdlDeleveragingPlan(
        BigDecimal requestedNotional,
        BigDecimal plannedNotional,
        BigDecimal remainingNotional,
        List<AdlDeleveragingStep> steps
) {
}
