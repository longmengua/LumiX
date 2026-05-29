/*
 * 檔案用途：UseCase 測試，驗證 ADL forced execution 入口的 command transaction boundary。
 */
package com.example.exchange.application.usecase;

import com.example.exchange.application.command.ExecuteAdlCommand;
import com.example.exchange.application.service.AdlForcedExecutionService;
import com.example.exchange.application.service.CommandTransactionBoundary;
import com.example.exchange.domain.model.dto.AdlDeleveragingPlan;
import com.example.exchange.domain.model.dto.AdlExecutionResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExecuteAdlUseCaseTest {

    @Test
    @DisplayName("ADL forced execution 入口會進入 command transaction boundary")
    void executeAdlUsesCommandTransactionBoundaryWhenConfigured() {
        RecordingAdlForcedExecutionService service = new RecordingAdlForcedExecutionService(false);
        RecordingTransactionManager transactionManager = new RecordingTransactionManager();
        ExecuteAdlUseCase useCase = new ExecuteAdlUseCase(service);
        useCase.setCommandTransactionBoundary(new CommandTransactionBoundary(new TransactionTemplate(transactionManager)));

        AdlExecutionResult result = useCase.handle(new ExecuteAdlCommand("adl-boundary", emptyPlan()));

        // ADL execution 會同時寫 position、ledger、execution record 與 audit event，入口必須有單一 command transaction。
        assertThat(result.commandId()).isEqualTo("adl-boundary");
        assertThat(service.calls).isEqualTo(1);
        assertThat(transactionManager.commits).isEqualTo(1);
        assertThat(transactionManager.rollbacks).isZero();
    }

    @Test
    @DisplayName("ADL forced execution 失敗會 rollback command transaction")
    void executeAdlRollsBackBoundaryOnFailure() {
        RecordingAdlForcedExecutionService service = new RecordingAdlForcedExecutionService(true);
        RecordingTransactionManager transactionManager = new RecordingTransactionManager();
        ExecuteAdlUseCase useCase = new ExecuteAdlUseCase(service);
        useCase.setCommandTransactionBoundary(new CommandTransactionBoundary(new TransactionTemplate(transactionManager)));

        assertThatThrownBy(() -> useCase.handle(new ExecuteAdlCommand("adl-fail", emptyPlan())))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ADL failed");

        assertThat(service.calls).isEqualTo(1);
        assertThat(transactionManager.commits).isZero();
        assertThat(transactionManager.rollbacks).isEqualTo(1);
    }

    private static AdlDeleveragingPlan emptyPlan() {
        return new AdlDeleveragingPlan(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, List.of());
    }

    /**
     * 測試替身：只記錄 ExecuteAdlUseCase 是否把 command 交給 ADL execution service。
     */
    private static final class RecordingAdlForcedExecutionService extends AdlForcedExecutionService {
        private final boolean fail;
        private int calls;

        private RecordingAdlForcedExecutionService(boolean fail) {
            super(null, null, null, null, event -> {
            });
            this.fail = fail;
        }

        @Override
        public AdlExecutionResult execute(String commandId, AdlDeleveragingPlan plan) {
            calls++;
            if (fail) {
                throw new IllegalStateException("ADL failed");
            }
            return new AdlExecutionResult(
                    commandId,
                    true,
                    "EXECUTED",
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    List.of(),
                    Instant.EPOCH
            );
        }
    }

    /**
     * 測試專用 transaction manager；用 commit/rollback 計數確認 use case 入口進入 boundary。
     */
    private static final class RecordingTransactionManager extends AbstractPlatformTransactionManager {
        private int commits;
        private int rollbacks;

        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
            // No real database is required for this boundary lifecycle test.
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
