/*
 * 檔案用途：領域事件，記錄 ADL forced execution 判斷與結果以供 audit。
 */
package com.example.exchange.domain.event;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * ADL forced deleveraging audit event。
 */
public record AdlForcedDeleveragingRecorded(
        String commandId,
        boolean executed,
        String reason,
        BigDecimal requestedNotional,
        BigDecimal plannedNotional,
        BigDecimal executedNotional,
        BigDecimal socializedLossCharged,
        Instant recordedAt
) {
}
