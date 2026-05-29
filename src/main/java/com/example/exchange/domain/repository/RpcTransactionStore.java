/*
 * 檔案用途：Repository 介面，定義 backend-observed RPC transaction idempotency / lifecycle 儲存契約。
 */
package com.example.exchange.domain.repository;

import com.example.exchange.domain.model.dto.RpcTransactionRecord;

import java.util.List;
import java.util.Optional;

public interface RpcTransactionStore {

    Optional<RpcTransactionRecord> find(String commandId);

    boolean claim(
            String commandId,
            String chainId,
            String transactionType,
            String walletAddress,
            String fingerprint,
            String txHash
    );

    RpcTransactionRecord complete(
            String commandId,
            String status,
            String lastError
    );

    List<RpcTransactionRecord> findUnresolved(int limit);
}
