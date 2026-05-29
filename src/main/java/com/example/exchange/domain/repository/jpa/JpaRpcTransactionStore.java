/*
 * 檔案用途：JPA adapter，實作 RPC transaction idempotency / lifecycle store。
 */
package com.example.exchange.domain.repository.jpa;

import com.example.exchange.domain.model.dto.RpcTransactionRecord;
import com.example.exchange.domain.model.entity.RpcTransactionRecordEntity;
import com.example.exchange.domain.repository.RpcTransactionStore;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JpaRpcTransactionStore implements RpcTransactionStore {

    private final RpcTransactionRecordRepository repository;

    @Override
    @Transactional(readOnly = true)
    public Optional<RpcTransactionRecord> find(String commandId) {
        if (commandId == null || commandId.isBlank()) {
            return Optional.empty();
        }
        return repository.findById(commandId.trim())
                .map(RpcTransactionRecordEntity::toRecord);
    }

    @Override
    @Transactional
    public boolean claim(
            String commandId,
            String chainId,
            String transactionType,
            String walletAddress,
            String fingerprint,
            String txHash
    ) {
        String normalizedCommandId =
                commandId.trim();
        if (repository.existsById(normalizedCommandId)) {
            return false;
        }

        try {
            repository.save(RpcTransactionRecordEntity.claimed(
                    normalizedCommandId,
                    chainId,
                    transactionType,
                    walletAddress,
                    fingerprint,
                    txHash,
                    Instant.now()
            ));
            return true;
        } catch (DataIntegrityViolationException duplicate) {
            return false;
        }
    }

    @Override
    @Transactional
    public RpcTransactionRecord complete(
            String commandId,
            String status,
            String lastError
    ) {
        RpcTransactionRecordEntity entity =
                repository.findById(commandId.trim())
                        .orElseThrow(() -> new IllegalStateException("RPC transaction claim missing: " + commandId));
        entity.setStatus(status);
        entity.setLastError(lastError);
        entity.setCompleted("CONFIRMED".equalsIgnoreCase(status)
                || "FAILED".equalsIgnoreCase(status));
        entity.setUpdatedAt(Instant.now());
        return repository.save(entity).toRecord();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RpcTransactionRecord> findUnresolved(int limit) {
        int cappedLimit =
                Math.max(1, Math.min(limit, 500));
        return repository.findByCompletedFalseOrderByUpdatedAtAsc()
                .stream()
                .limit(cappedLimit)
                .map(RpcTransactionRecordEntity::toRecord)
                .toList();
    }
}
