/*
 * 檔案用途：RPC transaction unresolved outcome report item。
 */
package com.example.exchange.domain.model.dto;

import java.time.Instant;

public record RpcTransactionIssue(
        String commandId,
        String reason,
        String chainId,
        String transactionType,
        String walletAddress,
        String txHash,
        String status,
        Instant updatedAt
) {
}
