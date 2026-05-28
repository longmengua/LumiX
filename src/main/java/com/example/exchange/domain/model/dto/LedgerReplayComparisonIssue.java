/*
 * 檔案用途：ledger replay comparison 的單一 component mismatch。
 */
package com.example.exchange.domain.model.dto;

import java.math.BigDecimal;

public record LedgerReplayComparisonIssue(
        String component,
        BigDecimal accountValue,
        BigDecimal replayValue,
        BigDecimal delta
) {
}
