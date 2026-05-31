/*
 * 檔案用途：應用服務測試，驗證核心指令交易邊界的 commit / rollback 行為。
 */
package com.example.exchange.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CommandTransactionBoundaryTest {

    @Test
    @DisplayName("成功的核心指令會在同一個 transaction 內 commit")
    /**
     * 流程：execute 開啟 transaction -> 執行 command body -> body 成功回傳 -> transaction commit。
     */
    void commitsSuccessfulCommand() {
        RecordingTransactionManager transactionManager = new RecordingTransactionManager();
        CommandTransactionBoundary boundary = new CommandTransactionBoundary(new TransactionTemplate(transactionManager));

        String result = boundary.execute("place-order", () -> "ok");

        assertThat(result).isEqualTo("ok");
        assertThat(transactionManager.commits).isEqualTo(1);
        assertThat(transactionManager.rollbacks).isZero();
    }

    @Test
    @DisplayName("核心指令失敗時會 rollback 並讓呼叫端自行 retry")
    /**
     * 流程：execute 開啟 transaction -> command body 拋錯 -> transaction rollback -> 原錯誤往外丟。
     */
    void rollsBackFailedCommandWithoutRetryingInsideBoundary() {
        RecordingTransactionManager transactionManager = new RecordingTransactionManager();
        CommandTransactionBoundary boundary = new CommandTransactionBoundary(new TransactionTemplate(transactionManager));
        AtomicInteger attempts = new AtomicInteger();

        assertThatThrownBy(() -> boundary.execute("place-order", () -> {
            attempts.incrementAndGet();
            throw new IllegalStateException("reserve failed");
        })).isInstanceOf(IllegalStateException.class)
                .hasMessage("reserve failed");

        // Retry/idempotency 屬於 command handler 的責任；交易邊界只保證失敗不 commit。
        assertThat(attempts).hasValue(1);
        assertThat(transactionManager.commits).isZero();
        assertThat(transactionManager.rollbacks).isEqualTo(1);
    }

    @Test
    @DisplayName("order place 的 outbox insert 失敗時會 rollback order persistence")
    void orderPlaceRollsBackWhenOutboxInsertFails() {
        RecordingTransactionManager transactionManager = new RecordingTransactionManager();
        TransactionalList<String> orders = new TransactionalList<>(transactionManager);
        TransactionalList<String> outboxRows = new TransactionalList<>(transactionManager);
        CommandTransactionBoundary boundary = new CommandTransactionBoundary(new TransactionTemplate(transactionManager));

        assertThatThrownBy(() -> boundary.execute("place-order", () -> {
            orders.add("order-1");
            outboxRows.failNextAdd("outbox insert failed");
            outboxRows.add("order.lifecycle:order-1");
            return null;
        })).isInstanceOf(IllegalStateException.class)
                .hasMessage("outbox insert failed");

        // DB 狀態與 outbox row 必須同 transaction；outbox insert 失敗不能留下半筆 order。
        assertThat(orders.committed()).isEmpty();
        assertThat(outboxRows.committed()).isEmpty();
        assertThat(transactionManager.rollbacks).isEqualTo(1);
    }

    @Test
    @DisplayName("cancel order 的 ledger release 失敗時會 rollback cancel state")
    void cancelOrderRollsBackWhenLedgerReleaseFails() {
        RecordingTransactionManager transactionManager = new RecordingTransactionManager();
        TransactionalList<String> orders = new TransactionalList<>(transactionManager);
        TransactionalList<String> ledger = new TransactionalList<>(transactionManager);
        CommandTransactionBoundary boundary = new CommandTransactionBoundary(new TransactionTemplate(transactionManager));

        assertThatThrownBy(() -> boundary.execute("cancel-order", () -> {
            orders.add("order-1:CANCELED");
            ledger.failNextAdd("ledger release failed");
            ledger.add("release-reserve:order-1");
            return null;
        })).isInstanceOf(IllegalStateException.class)
                .hasMessage("ledger release failed");

        // 撤單狀態與 reserve release 必須一起成功；ledger 失敗時不能留下 CANCELED hot/durable state。
        assertThat(orders.committed()).isEmpty();
        assertThat(ledger.committed()).isEmpty();
        assertThat(transactionManager.rollbacks).isEqualTo(1);
    }

    @Test
    @DisplayName("hedge execution 的 audit/outbox persistence 失敗時會 rollback hedge decision")
    void hedgeExecutionRollsBackWhenAuditOutboxPersistenceFails() {
        RecordingTransactionManager transactionManager = new RecordingTransactionManager();
        TransactionalList<String> hedgeDecisions = new TransactionalList<>(transactionManager);
        TransactionalList<String> auditOutbox = new TransactionalList<>(transactionManager);
        CommandTransactionBoundary boundary = new CommandTransactionBoundary(new TransactionTemplate(transactionManager));

        assertThatThrownBy(() -> boundary.execute("hedge-execution", () -> {
            hedgeDecisions.add("decision-1:ROUTE");
            auditOutbox.failNextAdd("audit outbox persistence failed");
            auditOutbox.add("hedge.decision:decision-1");
            return null;
        })).isInstanceOf(IllegalStateException.class)
                .hasMessage("audit outbox persistence failed");

        // 對沖決策沒有 audit/outbox row 就不能 commit，否則後續 reconciliation 無法重建決策來源。
        assertThat(hedgeDecisions.committed()).isEmpty();
        assertThat(auditOutbox.committed()).isEmpty();
        assertThat(transactionManager.rollbacks).isEqualTo(1);
    }

    /**
     * 測試專用 transaction manager；只記錄 commit/rollback 次數，不連接真實資料庫。
     */
    private static class RecordingTransactionManager extends AbstractPlatformTransactionManager {
        private int commits;
        private int rollbacks;
        private final List<Runnable> afterCommit = new ArrayList<>();
        private final List<Runnable> afterRollback = new ArrayList<>();

        private void onCommit(Runnable action) {
            afterCommit.add(action);
        }

        private void onRollback(Runnable action) {
            afterRollback.add(action);
        }

        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
            // No resource is bound; the test only cares about transaction lifecycle decisions.
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
            commits++;
            afterCommit.forEach(Runnable::run);
            clearCallbacks();
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
            rollbacks++;
            afterRollback.forEach(Runnable::run);
            clearCallbacks();
        }

        private void clearCallbacks() {
            afterCommit.clear();
            afterRollback.clear();
        }
    }

    private static class TransactionalList<T> {
        private final RecordingTransactionManager transactionManager;
        private final List<T> committed = new ArrayList<>();
        private final List<T> pending = new ArrayList<>();
        private String failNextAddMessage;

        private TransactionalList(RecordingTransactionManager transactionManager) {
            this.transactionManager = transactionManager;
            transactionManager.onCommit(() -> {
                committed.addAll(pending);
                pending.clear();
            });
            transactionManager.onRollback(pending::clear);
        }

        private void add(T value) {
            if (failNextAddMessage != null) {
                String message = failNextAddMessage;
                failNextAddMessage = null;
                throw new IllegalStateException(message);
            }
            pending.add(value);
        }

        private void failNextAdd(String message) {
            failNextAddMessage = message;
        }

        private List<T> committed() {
            return List.copyOf(committed);
        }
    }
}
