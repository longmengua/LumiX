/*
 * 檔案用途：RPC transaction unresolved outcome report item。
 */
package com.example.exchange.domain.model.dto;

import java.time.Instant;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;


@Data
@Builder
@Jacksonized
public class RpcTransactionIssue {

    private final String commandId;

    private final String reason;

    private final String chainId;

    private final String transactionType;

    private final String walletAddress;

    private final String txHash;

    private final String status;

    private final Instant updatedAt;
    public RpcTransactionIssue(String commandId, String reason, String chainId, String transactionType, String walletAddress, String txHash, String status, Instant updatedAt) {
        this.commandId = commandId;
        this.reason = reason;
        this.chainId = chainId;
        this.transactionType = transactionType;
        this.walletAddress = walletAddress;
        this.txHash = txHash;
        this.status = status;
        this.updatedAt = updatedAt;
    }

    public String commandId() {
        return commandId;
    }

    public String reason() {
        return reason;
    }

    public String chainId() {
        return chainId;
    }

    public String transactionType() {
        return transactionType;
    }

    public String walletAddress() {
        return walletAddress;
    }

    public String txHash() {
        return txHash;
    }

    public String status() {
        return status;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}