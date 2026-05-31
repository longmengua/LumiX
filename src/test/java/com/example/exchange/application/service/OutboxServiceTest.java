/*
 * 檔案用途：應用服務測試，驗證 outbox retry、DLQ 重放與人工補償。
 */
package com.example.exchange.application.service;

import com.example.exchange.domain.model.entity.DlqEvent;
import com.example.exchange.domain.model.entity.OutboxEvent;
import com.example.exchange.domain.repository.DlqRepository;
import com.example.exchange.domain.repository.OutboxRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 覆蓋 OutboxService 的可靠投遞基線：publish 失敗後重試、達上限進 DLQ、
 * dead event replay、人工補償，以及 trace headers 保存。
 */
class OutboxServiceTest {

    @Test
    @DisplayName("發布連續失敗會進 DLQ，replay 後可重新發布成功")
    /**
     * 流程：publish 失敗 -> relay 重試到 max attempts -> 進 DLQ -> replay dead event -> 再 relay 成功。
     */
    void failedEventMovesToDlqAndCanBeReplayed() {
        MemOutboxRepository outboxRepository = new MemOutboxRepository();
        MemDlqRepository dlqRepository = new MemDlqRepository();
        OutboxService service = new OutboxService(outboxRepository, dlqRepository);
        AtomicInteger attempts = new AtomicInteger();
        List<Object> published = new ArrayList<>();

        service.publish("orders", "BTCUSDT", "payload", (topic, key, payload, headers) -> {
            attempts.incrementAndGet();
            throw new IllegalStateException("broker unavailable");
        });
        // 第一次 publish 失敗只進入 retry，還不應進 DLQ。
        OutboxEvent event = outboxRepository.onlyEvent();
        assertThat(event.getStatus()).isEqualTo(OutboxEvent.Status.PENDING);
        assertThat(event.getAttempts()).isEqualTo(1);

        event.setNextAttemptAt(Instant.EPOCH);
        outboxRepository.save(event);
        service.relayDue(10, (topic, key, payload, headers) -> {
            attempts.incrementAndGet();
            throw new IllegalStateException("broker unavailable");
        });
        event.setNextAttemptAt(Instant.EPOCH);
        outboxRepository.save(event);
        service.relayDue(10, (topic, key, payload, headers) -> {
            attempts.incrementAndGet();
            throw new IllegalStateException("broker unavailable");
        });

        assertThat(event.getStatus()).isEqualTo(OutboxEvent.Status.DEAD);
        assertThat(event.getAttempts()).isEqualTo(3);
        assertThat(service.latestDlq(10)).hasSize(1);
        assertThat(service.latestDlq(10).getFirst().getOutboxId()).isEqualTo(event.getId());

        OutboxEvent replayed = service.replayDead(event.getId());
        assertThat(replayed.getStatus()).isEqualTo(OutboxEvent.Status.PENDING);
        assertThat(replayed.getAttempts()).isZero();
        assertThat(replayed.getLastError()).isNull();

        int relayed = service.relayDue(10, (topic, key, payload, headers) -> published.add(payload));
        assertThat(relayed).isEqualTo(1);
        assertThat(published).containsExactly("payload");
        assertThat(event.getStatus()).isEqualTo(OutboxEvent.Status.PUBLISHED);
        assertThat(attempts).hasValue(3);
    }

    @Test
    @DisplayName("死信事件可被標記為人工補償完成")
    /**
     * 流程：先放入 DEAD outbox event -> markCompensated -> 驗證狀態改為 COMPENSATED 且 relay 不再送出。
     */
    void deadEventCanBeMarkedCompensated() {
        MemOutboxRepository outboxRepository = new MemOutboxRepository();
        MemDlqRepository dlqRepository = new MemDlqRepository();
        OutboxService service = new OutboxService(outboxRepository, dlqRepository);

        OutboxEvent event = OutboxEvent.builder()
                .topic("orders")
                .eventKey("BTCUSDT")
                .eventType(String.class.getName())
                .payload("payload")
                .status(OutboxEvent.Status.DEAD)
                .attempts(3)
                .lastError("broker unavailable")
                .build();
        outboxRepository.save(event);

        OutboxEvent compensated = service.markCompensated(event.getId(), "rebuilt projection manually");

        assertThat(compensated.getStatus()).isEqualTo(OutboxEvent.Status.COMPENSATED);
        assertThat(compensated.getLastError()).isEqualTo("COMPENSATED: rebuilt projection manually");
        assertThat(service.relayDue(10, (topic, key, payload, headers) -> {
        })).isZero();
    }

    @Test
    @DisplayName("trace headers 會跟 outbox event 一起保存並在 replay 時帶出")
    /**
     * 流程：publish 時帶 trace headers -> 首次失敗仍保存 headers -> relay replay 時再次帶出同一組 headers。
     */
    void traceHeadersAreStoredAndReplayedWithOutboxEvent() {
        MemOutboxRepository outboxRepository = new MemOutboxRepository();
        MemDlqRepository dlqRepository = new MemDlqRepository();
        OutboxService service = new OutboxService(outboxRepository, dlqRepository);
        List<Map<String, String>> publishedHeaders = new ArrayList<>();

        service.publish(
                "orders",
                "BTCUSDT",
                "payload",
                Map.of("X-Request-Id", "req-1", "X-Correlation-Id", "corr-1"),
                (topic, key, payload, headers) -> {
                    publishedHeaders.add(headers);
                    throw new IllegalStateException("broker unavailable");
                }
        );

        OutboxEvent event = outboxRepository.onlyEvent();
        assertThat(event.getHeaders()).containsEntry("X-Request-Id", "req-1");
        assertThat(publishedHeaders).singleElement()
                .satisfies(headers -> assertThat(headers).containsEntry("X-Correlation-Id", "corr-1"));

        event.setNextAttemptAt(Instant.EPOCH);
        outboxRepository.save(event);
        service.relayDue(10, (topic, key, payload, headers) -> publishedHeaders.add(headers));

        assertThat(publishedHeaders).hasSize(2);
        assertThat(publishedHeaders.getLast()).containsEntry("X-Request-Id", "req-1");
    }

    @Test
    @DisplayName("transaction active 時 outbox 只先落庫，外部發布延到 commit 後")
    /**
     * 流程：模擬 Spring transaction active -> publish 只保存 outbox row -> afterCommit 才呼叫外部 publisher。
     */
    void publishDefersExternalSendUntilAfterCommitWhenTransactionIsActive() {
        MemOutboxRepository outboxRepository = new MemOutboxRepository();
        MemDlqRepository dlqRepository = new MemDlqRepository();
        OutboxService service = new OutboxService(outboxRepository, dlqRepository);
        List<Object> published = new ArrayList<>();

        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
        try {
            service.publish("orders", "BTCUSDT", "payload", (topic, key, payload, headers) -> published.add(payload));

            // 交易尚未 commit 前，資料庫 outbox row 是 authoritative record，Kafka 不應先看到事件。
            assertThat(outboxRepository.onlyEvent().getStatus()).isEqualTo(OutboxEvent.Status.PENDING);
            assertThat(published).isEmpty();

            for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
                synchronization.afterCommit();
            }

            assertThat(published).containsExactly("payload");
            assertThat(outboxRepository.onlyEvent().getStatus()).isEqualTo(OutboxEvent.Status.PUBLISHED);
        } finally {
            TransactionSynchronizationManager.setActualTransactionActive(false);
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    /**
     * 測試專用 outbox repository；保留事件順序，讓 retry/DLQ 狀態轉換可被直接斷言。
     */
    private static class MemOutboxRepository implements OutboxRepository {
        private final Map<UUID, OutboxEvent> events = new LinkedHashMap<>();

        @Override
        /**
         * 保存 outbox event；同 id 覆寫可模擬狀態從 PENDING -> DEAD -> PUBLISHED 的變化。
         */
        public void save(OutboxEvent event) {
            events.put(event.getId(), event);
        }

        @Override
        /**
         * 依 id 查回事件，replayDead 會用這個入口找到 dead event。
         */
        public Optional<OutboxEvent> findById(UUID id) {
            return Optional.ofNullable(events.get(id));
        }

        @Override
        /**
         * 找出到期的 PENDING event，讓 relayDue 只處理可以重送的事件。
         */
        public List<OutboxEvent> findDue(Instant now, int limit) {
            return events.values().stream()
                    .filter(event -> event.getStatus() == OutboxEvent.Status.PENDING)
                    .filter(event -> event.getNextAttemptAt() == null || !event.getNextAttemptAt().isAfter(now))
                    .limit(Math.max(1, limit))
                    .toList();
        }

        @Override
        public List<OutboxEvent> latest(int limit) {
            return events.values().stream().limit(Math.max(1, limit)).toList();
        }

        /**
         * 測試輔助方法：確保目前只有一筆 outbox event，方便直接檢查狀態轉換。
         */
        private OutboxEvent onlyEvent() {
            assertThat(events).hasSize(1);
            return events.values().iterator().next();
        }
    }

    /**
     * 測試專用 DLQ repository；新事件放最前面以模擬 latest 查詢。
     */
    private static class MemDlqRepository implements DlqRepository {
        private final List<DlqEvent> events = new ArrayList<>();

        @Override
        /**
         * 追加 dead-letter event；放到 list 前端以符合 latest 先回最新的語意。
         */
        public void append(DlqEvent event) {
            events.add(0, event);
        }

        @Override
        /**
         * 回傳最新 DLQ events，供測試確認失敗事件真的進入 dead-letter queue。
         */
        public List<DlqEvent> latest(int limit) {
            return events.stream().limit(Math.max(1, limit)).toList();
        }
    }
}
