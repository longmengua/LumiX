/*
 * 檔案用途：測試 backend-observed RPC transaction idempotency / lifecycle tracking。
 */
package com.example.exchange.application.service;

import com.example.exchange.domain.model.dto.RpcTransactionRecord;
import com.example.exchange.domain.model.dto.RpcTransactionReport;
import com.example.exchange.domain.repository.RpcTransactionStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RpcTransactionTrackingServiceTest {

    @Test
    @DisplayName("相同 RPC command 重送會 replay 既有 txHash，不建立第二筆")
    /**
     * 流程：backend 觀測到 approval relayer / RPC tx 後，用同一 commandId 重送相同 fingerprint。
     * 期望：第二次回傳既有 tx record，避免同一外部寫入 identity 指向多個 transaction。
     */
    void duplicateSubmittedCommandReplaysExistingTransaction() {
        MemStore store =
                new MemStore();
        RpcTransactionTrackingService service =
                new RpcTransactionTrackingService(store);

        RpcTransactionRecord first =
                service.observeSubmitted(
                        "approve-1",
                        "137",
                        "POLYMARKET_APPROVAL",
                        "0x0000000000000000000000000000000000000001",
                        "owner:spender:token:amount",
                        "0xabc"
                );
        RpcTransactionRecord second =
                service.observeSubmitted(
                        "approve-1",
                        "137",
                        "POLYMARKET_APPROVAL",
                        "0x0000000000000000000000000000000000000001",
                        "owner:spender:token:amount",
                        "0xabc"
                );

        assertThat(second)
                .isEqualTo(first);
        assertThat(store.records)
                .hasSize(1);
    }

    @Test
    @DisplayName("相同 RPC command 但 fingerprint 或 txHash 不同會拒絕")
    /**
     * 流程：client 或 relayer bug 重用 commandId，但 payload / txHash 已改變。
     * 期望：服務拒絕 idempotency conflict，避免同一 command identity 產生不同鏈上效果。
     */
    void sameCommandWithDifferentEffectIsConflict() {
        RpcTransactionTrackingService service =
                new RpcTransactionTrackingService(new MemStore());

        service.observeSubmitted(
                "approve-1",
                "137",
                "POLYMARKET_APPROVAL",
                "0x0000000000000000000000000000000000000001",
                "fp-1",
                "0xabc"
        );

        assertThatThrownBy(() -> service.observeSubmitted(
                "approve-1",
                "137",
                "POLYMARKET_APPROVAL",
                "0x0000000000000000000000000000000000000001",
                "fp-2",
                "0xdef"
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("RPC transaction idempotency conflict");
    }

    @Test
    @DisplayName("unresolved 只列出尚未完成的 RPC transaction")
    /**
     * 流程：一筆 tx 已 submitted 但尚未確認，另一筆已 confirmed。
     * 期望：營運報告只列出 pending outcome，供後續 receipt lookup / 手動 reconciliation。
     */
    void unresolvedReportsOnlyIncompleteTransactions() {
        MemStore store =
                new MemStore();
        RpcTransactionTrackingService service =
                new RpcTransactionTrackingService(store);

        service.observeSubmitted("approve-pending", "137", "POLYMARKET_APPROVAL", "0x1", "fp-1", "0xaaa");
        service.observeSubmitted("approve-done", "137", "POLYMARKET_APPROVAL", "0x1", "fp-2", "0xbbb");
        service.markConfirmed("approve-done");

        RpcTransactionReport report =
                service.unresolved(20);

        assertThat(report.issueCount())
                .isEqualTo(1);
        assertThat(report.issues().getFirst().commandId())
                .isEqualTo("approve-pending");
        assertThat(report.issues().getFirst().reason())
                .isEqualTo("RPC_TRANSACTION_OUTCOME_UNRESOLVED");
    }

    private static class MemStore implements RpcTransactionStore {
        private final List<RpcTransactionRecord> records =
                new ArrayList<>();

        @Override
        public Optional<RpcTransactionRecord> find(String commandId) {
            return records.stream()
                    .filter(record -> record.commandId().equals(commandId))
                    .findFirst();
        }

        @Override
        public boolean claim(
                String commandId,
                String chainId,
                String transactionType,
                String walletAddress,
                String fingerprint,
                String txHash
        ) {
            if (find(commandId).isPresent()) {
                return false;
            }
            Instant now =
                    Instant.now();
            records.add(new RpcTransactionRecord(
                    commandId,
                    chainId,
                    transactionType,
                    walletAddress,
                    fingerprint,
                    txHash,
                    "SUBMITTED",
                    null,
                    false,
                    now,
                    now
            ));
            return true;
        }

        @Override
        public RpcTransactionRecord complete(
                String commandId,
                String status,
                String lastError
        ) {
            RpcTransactionRecord existing =
                    find(commandId)
                            .orElseThrow();
            RpcTransactionRecord updated =
                    new RpcTransactionRecord(
                            existing.commandId(),
                            existing.chainId(),
                            existing.transactionType(),
                            existing.walletAddress(),
                            existing.fingerprint(),
                            existing.txHash(),
                            status,
                            lastError,
                            true,
                            existing.createdAt(),
                            Instant.now()
                    );
            records.remove(existing);
            records.add(updated);
            return updated;
        }

        @Override
        public List<RpcTransactionRecord> findUnresolved(int limit) {
            return records.stream()
                    .filter(record -> !record.completed())
                    .limit(limit)
                    .toList();
        }
    }
}
