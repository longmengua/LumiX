/*
 * 檔案用途：測試 Polymarket user WebSocket gateway checkpoint 與 replay baseline。
 */
package com.example.exchange.domain.service;

import com.example.exchange.domain.model.dto.PolymarketUserWsEvent;
import com.example.exchange.domain.model.entity.PredictionPolymarketUserWsCheckpoint;
import com.example.exchange.domain.model.entity.PredictionPolymarketWsEvent;
import com.example.exchange.domain.repository.jpa.PredictionPolymarketUserWsCheckpointRepository;
import com.example.exchange.domain.repository.jpa.PredictionPolymarketWsEventRepository;
import com.example.exchange.infra.config.PolymarketConfigs;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;

import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PolymarketUserWebSocketServiceTest {

    private static final String WALLET =
            "0x0000000000000000000000000000000000000001";

    @Test
    @DisplayName("user WebSocket message publish 後會保存 durable checkpoint")
    /**
     * 流程：gateway 收到 Polymarket user-channel trade message，送 Kafka 後保存 stream checkpoint。
     * 期望：checkpoint 以 wallet stream 為 key，記錄 eventKey、eventType、receivedAt 與 raw payload。
     */
    void websocketMessagePersistsCheckpointAfterPublish() {
        CheckpointRepository checkpointRepository =
                new CheckpointRepository(null);
        EventRepository eventRepository =
                new EventRepository(List.of());
        KafkaTemplate<String, Object> kafkaTemplate =
                mock(KafkaTemplate.class);
        PolymarketUserWebSocketService service =
                service(kafkaTemplate, checkpointRepository, eventRepository);

        service.handleTextForTest("""
                {
                  "event_type": "trade",
                  "status": "MATCHED",
                  "condition_id": "condition-1",
                  "asset_id": "asset-1",
                  "order_id": "clob-1",
                  "trade_id": "trade-1"
                }
                """);

        PredictionPolymarketUserWsCheckpoint checkpoint =
                checkpointRepository.saved;
        assertThat(checkpoint.getStreamKey())
                .isEqualTo("user:" + WALLET);
        assertThat(checkpoint.getWalletAddress())
                .isEqualTo(WALLET);
        assertThat(checkpoint.getLastEventKey())
                .isEqualTo("trade:MATCHED:clob-1:trade-1:condition-1:asset-1");
        assertThat(checkpoint.getLastEventType())
                .isEqualTo("trade");
        assertThat(checkpoint.getLastReceivedAt())
                .isNotNull();
        assertThat(checkpoint.getLastPayload())
                .contains("\"order_id\":\"clob-1\"");

        verify(kafkaTemplate)
                .send(eq("polymarket.user.events"), eq("clob-1"), any(PolymarketUserWsEvent.class));
    }

    @Test
    @DisplayName("checkpoint replay 會補送已落庫 user events 並推進 checkpoint")
    /**
     * 流程：gateway restart 後從 durable checkpoint 讀取 last event，再查詢 checkpoint 之後的 event rows。
     * 期望：補送後續事件到 Kafka，並把 checkpoint 推進到最後一筆 replay event。
     */
    void replayPersistedEventsAfterCheckpointPublishesAndAdvancesCheckpoint() {
        LocalDateTime firstReceivedAt =
                LocalDateTime.of(2026, 6, 3, 2, 20);
        LocalDateTime secondReceivedAt =
                firstReceivedAt.plusSeconds(1);
        PredictionPolymarketUserWsCheckpoint checkpoint =
                checkpoint("trade:MATCHED:clob-1:trade-1:condition-1:asset-1", firstReceivedAt);
        PredictionPolymarketWsEvent replayEvent =
                event("trade:MATCHED:clob-2:trade-2:condition-2:asset-2", secondReceivedAt);
        CheckpointRepository checkpointRepository =
                new CheckpointRepository(checkpoint);
        EventRepository eventRepository =
                new EventRepository(List.of(replayEvent));
        KafkaTemplate<String, Object> kafkaTemplate =
                mock(KafkaTemplate.class);
        PolymarketUserWebSocketService service =
                service(kafkaTemplate, checkpointRepository, eventRepository);

        int replayed =
                service.replayPersistedEventsFromCheckpoint(10);

        assertThat(replayed)
                .isEqualTo(1);
        assertThat(eventRepository.lastReceivedAt)
                .isEqualTo(firstReceivedAt);
        assertThat(eventRepository.lastEventKey)
                .isEqualTo("trade:MATCHED:clob-1:trade-1:condition-1:asset-1");
        assertThat(eventRepository.pageSize)
                .isEqualTo(10);
        assertThat(checkpointRepository.saved.getLastEventKey())
                .isEqualTo("trade:MATCHED:clob-2:trade-2:condition-2:asset-2");
        assertThat(checkpointRepository.saved.getLastReceivedAt())
                .isEqualTo(secondReceivedAt);

        verify(kafkaTemplate)
                .send(eq("polymarket.user.events"), eq("clob-2"), any(PolymarketUserWsEvent.class));
    }

    @Test
    @DisplayName("status 會回報獨立 user WebSocket worker identity 與 durable checkpoint")
    /**
     * 流程：獨立 worker 部署時，營運需要從 status 判斷 instance identity、stream key 與 checkpoint 位置。
     * 期望：status 帶出 role、instance id、replay batch size 以及目前 checkpoint event。
     */
    void statusReportsWorkerRuntimeAndCheckpoint() {
        LocalDateTime receivedAt =
                LocalDateTime.of(2026, 6, 4, 10, 30);
        PredictionPolymarketUserWsCheckpoint checkpoint =
                checkpoint("trade:MATCHED:clob-1:trade-1:condition-1:asset-1", receivedAt);
        checkpoint.setLastEventType("trade");
        CheckpointRepository checkpointRepository =
                new CheckpointRepository(checkpoint);
        EventRepository eventRepository =
                new EventRepository(List.of());
        PolymarketConfigs configs =
                configs();
        configs.getWs().setUserWorkerRole("USER_WS_WORKER");
        configs.getWs().setUserWorkerInstanceId("poly-ws-a");
        configs.getWs().setUserReplayBatchSize(250);
        PolymarketUserWebSocketService service =
                service(mock(KafkaTemplate.class), checkpointRepository, eventRepository, configs);

        var status =
                service.status();

        assertThat(status.getWorkerRole()).isEqualTo("USER_WS_WORKER");
        assertThat(status.getWorkerInstanceId()).isEqualTo("poly-ws-a");
        assertThat(status.getStreamKey()).isEqualTo("user:" + WALLET);
        assertThat(status.getCheckpointEventKey())
                .isEqualTo("trade:MATCHED:clob-1:trade-1:condition-1:asset-1");
        assertThat(status.getCheckpointEventType()).isEqualTo("trade");
        assertThat(status.getCheckpointReceivedAt()).isNotNull();
        assertThat(status.getReplayBatchSize()).isEqualTo(250);
    }

    private static PolymarketUserWebSocketService service(
            KafkaTemplate<String, Object> kafkaTemplate,
            CheckpointRepository checkpointRepository,
            EventRepository eventRepository
    ) {
        PolymarketConfigs configs =
                configs();
        return service(kafkaTemplate, checkpointRepository, eventRepository, configs);
    }

    private static PolymarketUserWebSocketService service(
            KafkaTemplate<String, Object> kafkaTemplate,
            CheckpointRepository checkpointRepository,
            EventRepository eventRepository,
            PolymarketConfigs configs
    ) {
        return new PolymarketUserWebSocketService(
                new ObjectMapper(),
                configs,
                kafkaTemplate,
                checkpointRepository.proxy(),
                eventRepository.proxy()
        );
    }

    private static PolymarketConfigs configs() {
        PolymarketConfigs configs =
                new PolymarketConfigs();
        configs.getWallet().setFunderAddress(WALLET);
        return configs;
    }

    private static PredictionPolymarketUserWsCheckpoint checkpoint(
            String eventKey,
            LocalDateTime receivedAt
    ) {
        PredictionPolymarketUserWsCheckpoint checkpoint =
                new PredictionPolymarketUserWsCheckpoint();
        checkpoint.setStreamKey("user:" + WALLET);
        checkpoint.setWalletAddress(WALLET);
        checkpoint.setLastEventKey(eventKey);
        checkpoint.setLastReceivedAt(receivedAt);
        return checkpoint;
    }

    private static PredictionPolymarketWsEvent event(
            String eventKey,
            LocalDateTime receivedAt
    ) {
        PredictionPolymarketWsEvent event =
                new PredictionPolymarketWsEvent();
        event.setEventKey(eventKey);
        event.setEventType("trade");
        event.setStatus("MATCHED");
        event.setWalletAddress(WALLET);
        event.setMarket("condition-2");
        event.setAssetId("asset-2");
        event.setOrderId("clob-2");
        event.setTradeId("trade-2");
        event.setPayload("{\"order_id\":\"clob-2\",\"trade_id\":\"trade-2\"}");
        event.setReceivedAt(receivedAt);
        return event;
    }

    private static class CheckpointRepository {
        private PredictionPolymarketUserWsCheckpoint saved;

        private CheckpointRepository(PredictionPolymarketUserWsCheckpoint saved) {
            this.saved = saved;
        }

        private PredictionPolymarketUserWsCheckpointRepository proxy() {
            return (PredictionPolymarketUserWsCheckpointRepository) Proxy.newProxyInstance(
                    PredictionPolymarketUserWsCheckpointRepository.class.getClassLoader(),
                    new Class<?>[]{PredictionPolymarketUserWsCheckpointRepository.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "findByStreamKey" -> saved != null
                                && saved.getStreamKey().equals(args[0])
                                ? Optional.of(saved)
                                : Optional.empty();
                        case "save" -> {
                            saved = (PredictionPolymarketUserWsCheckpoint) args[0];
                            yield saved;
                        }
                        case "toString" -> "CheckpointRepository";
                        default -> throw new UnsupportedOperationException(method.getName());
                    }
            );
        }
    }

    private static class EventRepository {
        private final List<PredictionPolymarketWsEvent> events;
        private LocalDateTime lastReceivedAt;
        private String lastEventKey;
        private int pageSize;

        private EventRepository(List<PredictionPolymarketWsEvent> events) {
            this.events = new ArrayList<>(events);
        }

        private PredictionPolymarketWsEventRepository proxy() {
            return (PredictionPolymarketWsEventRepository) Proxy.newProxyInstance(
                    PredictionPolymarketWsEventRepository.class.getClassLoader(),
                    new Class<?>[]{PredictionPolymarketWsEventRepository.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "findReplayBatchAfterCheckpoint" -> {
                            lastReceivedAt = (LocalDateTime) args[1];
                            lastEventKey = (String) args[2];
                            pageSize = ((Pageable) args[3]).getPageSize();
                            yield events;
                        }
                        case "toString" -> "EventRepository";
                        default -> throw new UnsupportedOperationException(method.getName());
                    }
            );
        }
    }
}
