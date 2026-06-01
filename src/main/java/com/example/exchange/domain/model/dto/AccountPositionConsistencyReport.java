/*
 * 檔案用途：restore 後 account / position consistency validation report。
 */
package com.example.exchange.domain.model.dto;

import java.time.Instant;
import java.util.List;

public record AccountPositionConsistencyReport(
        int accountsChecked,
        int openPositionsChecked,
        int issueCount,
        boolean valid,
        Instant generatedAt,
        List<AccountPositionConsistencyIssue> issues
) {
    public AccountPositionConsistencyReport {
        generatedAt = generatedAt == null ? Instant.now() : generatedAt;
        issues = issues == null ? List.of() : List.copyOf(issues);
    }
}
