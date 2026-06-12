/*
 * 檔案用途：Kafka 基礎設施 adapter，負責 domain event 的路由、發布與保存。
 */
package com.example.exchange.infra.kafka;

import com.example.exchange.application.event.DomainEventPublisher;
import com.example.exchange.application.service.OrderLifecycleProjectionService;
import com.example.exchange.application.service.OutboxService;
import com.example.exchange.application.service.PushGatewayService;
import com.example.exchange.domain.event.OrderLifecycleEvent;
import com.example.exchange.infra.tracing.TraceContext;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 應用層事件發布器（Kafka）
 * - 依事件型別 routing 到對應 topic，並以 symbol 作為 key，維持同 symbol 有序。
 */
@Component
public class KafkaDomainEventPublisher<T> implements DomainEventPublisher<T> {

    private final KafkaTemplate<String, Object> kafka;
    private final OutboxService outboxService;
    private final OrderLifecycleProjectionService orderLifecycleProjectionService;
    private final PushGatewayService pushGatewayService;

    public KafkaDomainEventPublisher(
            KafkaTemplate<String, Object> kafka,
            OutboxService outboxService,
            OrderLifecycleProjectionService orderLifecycleProjectionService,
            PushGatewayService pushGatewayService
    ) {
        this.kafka = kafka;
        this.outboxService = outboxService;
        this.orderLifecycleProjectionService = orderLifecycleProjectionService;
        this.pushGatewayService = pushGatewayService;
    }

    @Override
    public void publish(T event) {
        if (event instanceof OrderLifecycleEvent orderLifecycleEvent) {
            orderLifecycleProjectionService.record(orderLifecycleEvent);
            // User-channel WebSocket clients use this event as the low-latency signal to refresh order state.
            pushGatewayService.publishUser(orderLifecycleEvent.uid(), "order.lifecycle", orderLifecycleEvent);
        }
        KafkaEventRoute route = KafkaEventRoute.from(event);
        outboxService.publish(route.topic(), route.key(), event, TraceContext.currentHeaders(), this::send);
    }

    private void send(String topic, String key, Object payload, Map<String, String> headers) throws Exception {
        ProducerRecord<String, Object> record = new ProducerRecord<>(topic, key, payload);
        addHeaders(record, headers);
        kafka.send(record).get(3, TimeUnit.SECONDS);
    }

    private static void addHeaders(ProducerRecord<String, Object> record, Map<String, String> headers) {
        if (headers == null || headers.isEmpty()) return;
        headers.forEach((name, value) -> {
            if (name == null || name.isBlank() || value == null || value.isBlank()) return;
            record.headers().add(name, value.getBytes(StandardCharsets.UTF_8));
        });
    }
}
