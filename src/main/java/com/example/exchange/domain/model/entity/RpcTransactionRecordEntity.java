/*
 * 檔案用途：JPA entity，保存 backend-observed RPC transaction idempotency / lifecycle 狀態。
 */
package com.example.exchange.domain.model.entity;

import com.example.exchange.domain.model.dto.RpcTransactionRecord;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(
        name = "rpc_transaction_record",
        indexes = {
                @Index(name = "idx_rpc_tx_hash", columnList = "tx_hash"),
                @Index(name = "idx_rpc_tx_wallet", columnList = "wallet_address"),
                @Index(name = "idx_rpc_tx_type_completed", columnList = "transaction_type,completed"),
                @Index(name = "idx_rpc_tx_status_updated", columnList = "status,updated_at")
        }
)
public class RpcTransactionRecordEntity {

    @Id
    @Column(name = "command_id", nullable = false, length = 128)
    private String commandId;

    @Column(name = "chain_id", nullable = false, length = 32)
    private String chainId;

    @Column(name = "transaction_type", nullable = false, length = 64)
    private String transactionType;

    @Column(name = "wallet_address", nullable = false, length = 64)
    private String walletAddress;

    @Column(name = "fingerprint", nullable = false, length = 512)
    private String fingerprint;

    @Column(name = "tx_hash", nullable = false, length = 128)
    private String txHash;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "completed", nullable = false)
    private Boolean completed = false;

    @Column(name = "created_at", nullable = false, columnDefinition = "DATETIME(6)")
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, columnDefinition = "DATETIME(6)")
    private Instant updatedAt;

    public static RpcTransactionRecordEntity claimed(
            String commandId,
            String chainId,
            String transactionType,
            String walletAddress,
            String fingerprint,
            String txHash,
            Instant now
    ) {
        RpcTransactionRecordEntity entity =
                new RpcTransactionRecordEntity();
        entity.setCommandId(commandId);
        entity.setChainId(chainId);
        entity.setTransactionType(transactionType);
        entity.setWalletAddress(walletAddress);
        entity.setFingerprint(fingerprint);
        entity.setTxHash(txHash);
        entity.setStatus("SUBMITTED");
        entity.setCompleted(false);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        return entity;
    }

    public RpcTransactionRecord toRecord() {
        return new RpcTransactionRecord(
                commandId,
                chainId,
                transactionType,
                walletAddress,
                fingerprint,
                txHash,
                status,
                lastError,
                Boolean.TRUE.equals(completed),
                createdAt,
                updatedAt
        );
    }
}
