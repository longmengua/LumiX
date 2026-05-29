/*
 * 檔案用途：UseCase 測試，驗證訂單指令入口的 command transaction boundary rollback。
 */
package com.example.exchange.application.usecase;

import com.example.exchange.application.command.CancelReplaceOrderCommand;
import com.example.exchange.application.service.CommandTransactionBoundary;
import com.example.exchange.domain.model.entity.Order;
import com.example.exchange.domain.repository.OrderRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderCommandTransactionBoundaryTest {

    @Test
    @DisplayName("place-order validation failure 會 rollback command transaction")
    void placeOrderValidationFailureRollsBackBoundary() {
        RecordingTransactionManager transactionManager = new RecordingTransactionManager();
        PlaceOrderUseCase useCase = new PlaceOrderUseCase(null, null, null, event -> {
        });
        useCase.setCommandTransactionBoundary(boundary(transactionManager));

        // 流程：即使失敗發生在 validation，仍需位於 command boundary 內，避免入口旁路交易規則。
        assertThatThrownBy(() -> useCase.place(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("place order command");

        assertThat(transactionManager.commits).isZero();
        assertThat(transactionManager.rollbacks).isEqualTo(1);
    }

    @Test
    @DisplayName("cancel-order missing order failure 會 rollback command transaction")
    void cancelOrderMissingOrderRollsBackBoundary() {
        RecordingTransactionManager transactionManager = new RecordingTransactionManager();
        CancelOrderUseCase useCase = new CancelOrderUseCase(
                new EmptyOrderRepository(),
                null,
                null,
                null,
                null,
                event -> {
                }
        );
        useCase.setCommandTransactionBoundary(boundary(transactionManager));

        // 流程：找不到訂單代表 command 失敗，不應 commit 任何後續帳務或 outbox side effect。
        assertThatThrownBy(() -> useCase.handle(UUID.randomUUID()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("order not found");

        assertThat(transactionManager.commits).isZero();
        assertThat(transactionManager.rollbacks).isEqualTo(1);
    }

    @Test
    @DisplayName("amend-order validation failure 會 rollback command transaction")
    void amendOrderValidationFailureRollsBackBoundary() {
        RecordingTransactionManager transactionManager = new RecordingTransactionManager();
        AmendOrderUseCase useCase = new AmendOrderUseCase(
                null,
                null,
                null,
                null,
                null,
                event -> {
                }
        );
        useCase.setCommandTransactionBoundary(boundary(transactionManager));

        // 流程：amend 缺 command 不能在 transaction 外快速失敗，否則測不到入口 boundary 是否存在。
        assertThatThrownBy(() -> useCase.handle(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("amend order command");

        assertThat(transactionManager.commits).isZero();
        assertThat(transactionManager.rollbacks).isEqualTo(1);
    }

    @Test
    @DisplayName("cancel-replace validation failure 會 rollback command transaction")
    void cancelReplaceValidationFailureRollsBackBoundary() {
        RecordingTransactionManager transactionManager = new RecordingTransactionManager();
        CancelReplaceOrderUseCase useCase = new CancelReplaceOrderUseCase(null, null, null);
        useCase.setCommandTransactionBoundary(boundary(transactionManager));

        // 流程：cancel-replace 是 cancel + replacement 的外層 command，入口 validation 也需走同一 boundary。
        assertThatThrownBy(() -> useCase.handle(new CancelReplaceOrderCommand(
                null,
                1L,
                null,
                null,
                null
        ))).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("orderId");

        assertThat(transactionManager.commits).isZero();
        assertThat(transactionManager.rollbacks).isEqualTo(1);
    }

    private static CommandTransactionBoundary boundary(RecordingTransactionManager transactionManager) {
        return new CommandTransactionBoundary(new TransactionTemplate(transactionManager));
    }

    private static final class EmptyOrderRepository implements OrderRepository {
        @Override
        public Optional<Order> findById(UUID id) {
            return Optional.empty();
        }

        @Override
        public void save(Order order) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Order> openOrders(long uid) {
            return List.of();
        }

        @Override
        public List<Order> findOpenOrders(Long uid, String symbol) {
            return List.of();
        }

        @Override
        public List<Order> findAllOrders(Long uid, String symbol) {
            return List.of();
        }
    }

    /**
     * 測試專用 transaction manager；只記錄 command boundary 是否 commit 或 rollback。
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
            // No real database is needed; the test verifies boundary lifecycle only.
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
