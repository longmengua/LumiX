/*
 * 檔案用途：做市商自動報價 DTO，彙總一次背景 loop 的報價結果。
 */
package com.example.exchange.domain.model.dto;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;


@Data
@Builder
@Jacksonized
public class MarketMakerAutoQuoteRunReport {

    private final long sequence;

    private final int placedCount;

    private final int skippedCount;

    private final List<MarketMakerAutoQuoteResult> results;
    public MarketMakerAutoQuoteRunReport(long sequence, int placedCount, int skippedCount, List<MarketMakerAutoQuoteResult> results) {
        results = results == null ? List.of() : List.copyOf(results);
    
        this.sequence = sequence;
        this.placedCount = placedCount;
        this.skippedCount = skippedCount;
        this.results = results;
    }

    public long sequence() {
        return sequence;
    }

    public int placedCount() {
        return placedCount;
    }

    public int skippedCount() {
        return skippedCount;
    }

    public List<MarketMakerAutoQuoteResult> results() {
        return results;
    }
}