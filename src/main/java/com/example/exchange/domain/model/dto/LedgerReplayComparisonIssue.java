/*
 * 檔案用途：ledger replay comparison 的單一 component mismatch。
 */
package com.example.exchange.domain.model.dto;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;


@Data
@Builder
@Jacksonized
public class LedgerReplayComparisonIssue {

    private final String component;

    private final BigDecimal accountValue;

    private final BigDecimal replayValue;

    private final BigDecimal delta;
    public LedgerReplayComparisonIssue(String component, BigDecimal accountValue, BigDecimal replayValue, BigDecimal delta) {
        this.component = component;
        this.accountValue = accountValue;
        this.replayValue = replayValue;
        this.delta = delta;
    }

    public String component() {
        return component;
    }

    public BigDecimal accountValue() {
        return accountValue;
    }

    public BigDecimal replayValue() {
        return replayValue;
    }

    public BigDecimal delta() {
        return delta;
    }
}