/*
 * 檔案用途：account/position restore consistency issue DTO。
 */
package com.example.exchange.domain.model.dto;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;


@Data
@Builder
@Jacksonized
public class AccountPositionConsistencyIssue {

    private final long uid;

    private final String symbol;

    private final String issueType;

    private final BigDecimal accountPositionMargin;

    private final BigDecimal openPositionMargin;

    private final String detail;
    public AccountPositionConsistencyIssue(long uid, String symbol, String issueType, BigDecimal accountPositionMargin, BigDecimal openPositionMargin, String detail) {
        this.uid = uid;
        this.symbol = symbol;
        this.issueType = issueType;
        this.accountPositionMargin = accountPositionMargin;
        this.openPositionMargin = openPositionMargin;
        this.detail = detail;
    }

    public long uid() {
        return uid;
    }

    public String symbol() {
        return symbol;
    }

    public String issueType() {
        return issueType;
    }

    public BigDecimal accountPositionMargin() {
        return accountPositionMargin;
    }

    public BigDecimal openPositionMargin() {
        return openPositionMargin;
    }

    public String detail() {
        return detail;
    }
}