package com.example.java21_OLAP.application.event;

/**
 * 應用層事件發布抽象
 * - 由 infra 提供實作（Kafka、RocketMQ…）
 * - 用例/服務可以透過它把領域事件「往外送」
 */
public interface DomainEventPublisher<T> {
    void publish(T event);
}
