/*
 * 檔案用途：Kafka 基礎設施 adapter，負責 domain event 的路由、發布與保存。
 */
package com.example.exchange.infra.kafka;

import com.example.exchange.application.event.DomainEventPublisher;
import com.example.exchange.application.service.OutboxService;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 應用層事件發布器（Kafka）
 * - 依事件型別 routing 到對應 topic，並以 symbol 作為 key，維持同 symbol 有序。
 */
@Component
public class KafkaDomainEventPublisher<T> implements DomainEventPublisher<T> {

    private final KafkaTemplate<String, Object> kafka;
    private final OutboxService outboxService;

    public KafkaDomainEventPublisher(KafkaTemplate<String, Object> kafka, OutboxService outboxService) {
        this.kafka = kafka;
        this.outboxService = outboxService;
    }

    @Override
    public void publish(T event) {
        KafkaEventRoute route = KafkaEventRoute.from(event);
        outboxService.publish(route.topic(), route.key(), event, (topic, key, payload) ->
                kafka.send(topic, key, payload).get(3, TimeUnit.SECONDS));
    }
}
