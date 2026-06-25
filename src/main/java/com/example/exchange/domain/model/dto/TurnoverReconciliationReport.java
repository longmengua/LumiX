/*
 * 檔案用途：Turnover 與 trade tape 對帳報告 DTO。
 */
package com.example.exchange.domain.model.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;


@Data
@Builder
@Jacksonized
public class TurnoverReconciliationReport {

    private final long uid;

    private final String matchId;

    private final long turnoverRecordCount;

    private final long tradeTapeRecordCount;

    private final BigDecimal turnoverNotional;

    private final BigDecimal tradeTapeNotional;

    private final int issueCount;

    private final Instant generatedAt;

    private final List<TurnoverReconciliationIssue> issues;
    public TurnoverReconciliationReport(long uid, String matchId, long turnoverRecordCount, long tradeTapeRecordCount, BigDecimal turnoverNotional, BigDecimal tradeTapeNotional, int issueCount, Instant generatedAt, List<TurnoverReconciliationIssue> issues) {
        turnoverNotional = turnoverNotional == null ? BigDecimal.ZERO : turnoverNotional;
        tradeTapeNotional = tradeTapeNotional == null ? BigDecimal.ZERO : tradeTapeNotional;
        issues = issues == null ? List.of() : List.copyOf(issues);
    
        this.uid = uid;
        this.matchId = matchId;
        this.turnoverRecordCount = turnoverRecordCount;
        this.tradeTapeRecordCount = tradeTapeRecordCount;
        this.turnoverNotional = turnoverNotional;
        this.tradeTapeNotional = tradeTapeNotional;
        this.issueCount = issueCount;
        this.generatedAt = generatedAt;
        this.issues = issues;
    }

    public long uid() {
        return uid;
    }

    public String matchId() {
        return matchId;
    }

    public long turnoverRecordCount() {
        return turnoverRecordCount;
    }

    public long tradeTapeRecordCount() {
        return tradeTapeRecordCount;
    }

    public BigDecimal turnoverNotional() {
        return turnoverNotional;
    }

    public BigDecimal tradeTapeNotional() {
        return tradeTapeNotional;
    }

    public int issueCount() {
        return issueCount;
    }

    public Instant generatedAt() {
        return generatedAt;
    }

    public List<TurnoverReconciliationIssue> issues() {
        return issues;
    }
}