/*
 * 檔案用途：應用服務，提供 backend-observed RPC transaction idempotency 與 unresolved outcome 檢視。
 */
package com.example.exchange.application.service;

import com.example.exchange.domain.model.dto.RpcTransactionIssue;
import com.example.exchange.domain.model.dto.RpcTransactionRecord;
import com.example.exchange.domain.model.dto.RpcTransactionReport;
import com.example.exchange.domain.repository.RpcTransactionStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class RpcTransactionTrackingService {

    private final RpcTransactionStore store;

    @Transactional
    public RpcTransactionRecord observeSubmitted(
            String commandId,
            String chainId,
            String transactionType,
            String walletAddress,
            String fingerprint,
            String txHash
    ) {
        validate(commandId, chainId, transactionType, walletAddress, fingerprint, txHash);

        String normalizedCommandId =
                commandId.trim();
        RpcTransactionRecord existing =
                store.find(normalizedCommandId).orElse(null);
        if (existing != null) {
            assertSameEffect(existing, fingerprint, txHash);
            return existing;
        }

        boolean claimed =
                store.claim(
                        normalizedCommandId,
                        chainId.trim(),
                        transactionType.trim(),
                        walletAddress.trim(),
                        fingerprint.trim(),
                        txHash.trim()
                );
        RpcTransactionRecord record =
                store.find(normalizedCommandId)
                        .orElseThrow(() -> new IllegalStateException("RPC transaction claim missing: " + normalizedCommandId));
        assertSameEffect(record, fingerprint, txHash);
        if (!claimed) {
            return record;
        }
        return record;
    }

    @Transactional
    public RpcTransactionRecord markConfirmed(String commandId) {
        requireCommandId(commandId);
        return store.complete(commandId.trim(), "CONFIRMED", null);
    }

    @Transactional
    public RpcTransactionRecord markFailed(
            String commandId,
            String lastError
    ) {
        requireCommandId(commandId);
        return store.complete(commandId.trim(), "FAILED", lastError);
    }

    @Transactional(readOnly = true)
    public RpcTransactionReport unresolved(int limit) {
        var issues = store.findUnresolved(limit)
                .stream()
                .map(this::toIssue)
                .toList();
        return new RpcTransactionReport(
                issues.size(),
                Instant.now(),
                issues
        );
    }

    private RpcTransactionIssue toIssue(RpcTransactionRecord record) {
        return new RpcTransactionIssue(
                record.commandId(),
                "RPC_TRANSACTION_OUTCOME_UNRESOLVED",
                record.chainId(),
                record.transactionType(),
                record.walletAddress(),
                record.txHash(),
                record.status(),
                record.updatedAt()
        );
    }

    private void assertSameEffect(
            RpcTransactionRecord existing,
            String fingerprint,
            String txHash
    ) {
        if (!Objects.equals(existing.fingerprint(), fingerprint.trim())
                || !Objects.equals(existing.txHash(), txHash.trim())) {
            throw new IllegalStateException(
                    "RPC transaction idempotency conflict: commandId="
                            + existing.commandId()
            );
        }
    }

    private void validate(
            String commandId,
            String chainId,
            String transactionType,
            String walletAddress,
            String fingerprint,
            String txHash
    ) {
        requireCommandId(commandId);
        requireText(chainId, "chainId is required");
        requireText(transactionType, "transactionType is required");
        requireText(walletAddress, "walletAddress is required");
        requireText(fingerprint, "fingerprint is required");
        requireText(txHash, "txHash is required");
    }

    private void requireCommandId(String commandId) {
        requireText(commandId, "commandId is required");
    }

    private void requireText(
            String value,
            String message
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }
}
