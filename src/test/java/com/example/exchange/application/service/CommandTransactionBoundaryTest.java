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

    /**
     * 測試專用 transaction manager；只記錄 commit/rollback 次數，不連接真實資料庫。
     */
    private static class RecordingTransactionManager extends AbstractPlatformTransactionManager {
        private int commits;
        private int rollbacks;

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
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
            rollbacks++;
        }
    }
}
