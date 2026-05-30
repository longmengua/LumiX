/*
 * 檔案用途：Turnover 與 trade tape 對帳報告 DTO。
 */
package com.example.exchange.domain.model.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record TurnoverReconciliationReport(
        long uid,
        String matchId,
        long turnoverRecordCount,
        long tradeTapeRecordCount,
        BigDecimal turnoverNotional,
        BigDecimal tradeTapeNotional,
        int issueCount,
        Instant generatedAt,
        List<TurnoverReconciliationIssue> issues
) {
    public TurnoverReconciliationReport {
        turnoverNotional = turnoverNotional == null ? BigDecimal.ZERO : turnoverNotional;
        tradeTapeNotional = tradeTapeNotional == null ? BigDecimal.ZERO : tradeTapeNotional;
        issues = issues == null ? List.of() : List.copyOf(issues);
    }
}
