/*
 * 檔案用途：RPC transaction unresolved outcome operator report。
 */
package com.example.exchange.domain.model.dto;

import java.time.Instant;
import java.util.List;

public record RpcTransactionReport(
        int issueCount,
        Instant generatedAt,
        List<RpcTransactionIssue> issues
) {
}
