/*
 * 檔案用途：RPC transaction unresolved outcome operator report。
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
public class RpcTransactionReport {

    private final int issueCount;

    private final Instant generatedAt;

    private final List<RpcTransactionIssue> issues;
    public RpcTransactionReport(int issueCount, Instant generatedAt, List<RpcTransactionIssue> issues) {
        this.issueCount = issueCount;
        this.generatedAt = generatedAt;
        this.issues = issues;
    }

    public int issueCount() {
        return issueCount;
    }

    public Instant generatedAt() {
        return generatedAt;
    }

    public List<RpcTransactionIssue> issues() {
        return issues;
    }
}