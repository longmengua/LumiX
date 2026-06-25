/*
 * 檔案用途：保存 backend-observed RPC transaction idempotency / lifecycle 狀態。
 */
package com.example.exchange.domain.model.dto;

import java.time.Instant;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;


@Data
@Builder
@Jacksonized
public class RpcTransactionRecord {

    private final String commandId;

    private final String chainId;

    private final String transactionType;

    private final String walletAddress;

    private final String fingerprint;

    private final String txHash;

    private final String status;

    private final String lastError;

    private final boolean completed;

    private final Instant createdAt;

    private final Instant updatedAt;
    public RpcTransactionRecord(String commandId, String chainId, String transactionType, String walletAddress, String fingerprint, String txHash, String status, String lastError, boolean completed, Instant createdAt, Instant updatedAt) {
        this.commandId = commandId;
        this.chainId = chainId;
        this.transactionType = transactionType;
        this.walletAddress = walletAddress;
        this.fingerprint = fingerprint;
        this.txHash = txHash;
        this.status = status;
        this.lastError = lastError;
        this.completed = completed;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String commandId() {
        return commandId;
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

    public String fingerprint() {
        return fingerprint;
    }

    public String txHash() {
        return txHash;
    }

    public String status() {
        return status;
    }

    public String lastError() {
        return lastError;
    }

    public boolean completed() {
        return completed;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}