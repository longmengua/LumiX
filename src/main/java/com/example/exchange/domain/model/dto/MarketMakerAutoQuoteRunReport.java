/*
 * 檔案用途：做市商自動報價 DTO，彙總一次背景 loop 的報價結果。
 */
package com.example.exchange.domain.model.dto;

import java.util.List;

public record MarketMakerAutoQuoteRunReport(
        long sequence,
        int placedCount,
        int skippedCount,
        List<MarketMakerAutoQuoteResult> results
) {
    public MarketMakerAutoQuoteRunReport {
        results = results == null ? List.of() : List.copyOf(results);
    }
}
