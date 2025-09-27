package com.example.exchange.infra.kafka;

import com.example.exchange.application.event.DomainEventPublisher;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * 應用層事件發布器（Kafka）
 * - 範例把所有事件都送到 "trade.executed" topic（你可依型別做 routing）
 */
@Component
public class KafkaDomainEventPublisher<T> implements DomainEventPublisher<T> {

    private final KafkaTemplate<String, Object> kafka;

    public KafkaDomainEventPublisher(KafkaTemplate<String, Object> kafka) {
        this.kafka = kafka;
    }

    @Override
    public void publish(T event) {
        kafka.send("trade.executed", event);
    }
}
