/*
 * 檔案用途：保存 backend-observed RPC transaction idempotency / lifecycle 狀態。
 */
package com.example.exchange.domain.model.dto;

import java.time.Instant;

public record RpcTransactionRecord(
        String commandId,
        String chainId,
        String transactionType,
        String walletAddress,
        String fingerprint,
        String txHash,
        String status,
        String lastError,
        boolean completed,
        Instant createdAt,
        Instant updatedAt
) {
}
