/*
 * 檔案用途：restore 後 account / position consistency validation report。
 */
package com.example.exchange.domain.model.dto;

import java.time.Instant;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;


@Data
@Builder
@Jacksonized
public class AccountPositionConsistencyReport {

    private final int accountsChecked;

    private final int openPositionsChecked;

    private final int issueCount;

    private final boolean valid;

    private final Instant generatedAt;

    private final List<AccountPositionConsistencyIssue> issues;
    public AccountPositionConsistencyReport(int accountsChecked, int openPositionsChecked, int issueCount, boolean valid, Instant generatedAt, List<AccountPositionConsistencyIssue> issues) {
        generatedAt = generatedAt == null ? Instant.now() : generatedAt;
        issues = issues == null ? List.of() : List.copyOf(issues);
    
        this.accountsChecked = accountsChecked;
        this.openPositionsChecked = openPositionsChecked;
        this.issueCount = issueCount;
        this.valid = valid;
        this.generatedAt = generatedAt;
        this.issues = issues;
    }

    public int accountsChecked() {
        return accountsChecked;
    }

    public int openPositionsChecked() {
        return openPositionsChecked;
    }

    public int issueCount() {
        return issueCount;
    }

    public boolean valid() {
        return valid;
    }

    public Instant generatedAt() {
        return generatedAt;
    }

    public List<AccountPositionConsistencyIssue> issues() {
        return issues;
    }
}