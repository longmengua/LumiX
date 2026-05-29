/*
 * 檔案用途：UseCase 測試，驗證手動強平入口的 command transaction boundary。
 */
package com.example.exchange.application.usecase;

import com.example.exchange.application.command.LiquidateCommand;
import com.example.exchange.application.service.CommandTransactionBoundary;
import com.example.exchange.application.service.LiquidationService;
import com.example.exchange.domain.model.dto.LiquidationResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class LiquidateUseCaseTest {

    @Test
    @DisplayName("manual liquidation 入口會進入 command transaction boundary")
    void manualLiquidationUsesCommandTransactionBoundaryWhenConfigured() {
        RecordingLiquidationService liquidationService = new RecordingLiquidationService();
        RecordingTransactionManager transactionManager = new RecordingTransactionManager();
        LiquidateUseCase useCase = new LiquidateUseCase(liquidationService);
        useCase.setCommandTransactionBoundary(new CommandTransactionBoundary(new TransactionTemplate(transactionManager)));

        LiquidationResult result = useCase.handle(new LiquidateCommand(7, "BTCUSDT", new BigDecimal("1.00")));

        // 強平會改 position、ledger、insurance/ADL queue 並發布 audit event，因此入口必須有明確 transaction boundary。
        assertThat(result.uid()).isEqualTo(7);
        assertThat(liquidationService.explicitMarkPriceCalls).isEqualTo(1);
        assertThat(transactionManager.commits).isEqualTo(1);
        assertThat(transactionManager.rollbacks).isZero();
    }

    /**
     * 測試替身：只記錄 LiquidateUseCase 是否用 explicit mark price 進入 liquidation service。
     */
    private static final class RecordingLiquidationService extends LiquidationService {
        private int explicitMarkPriceCalls;

        private RecordingLiquidationService() {
            super(null, null, null, null, null, event -> {
            });
        }

        @Override
        public LiquidationResult liquidate(long uid, String symbol, BigDecimal markPrice) {
            explicitMarkPriceCalls++;
            return new LiquidationResult(
                    uid,
                    symbol,
                    true,
                    markPrice,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    "liq-test",
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
