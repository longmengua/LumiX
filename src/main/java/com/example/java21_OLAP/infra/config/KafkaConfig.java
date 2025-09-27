package com.example.java21_OLAP.infra.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Kafka 主題建立（簡單示範）
 * - 生產環境通常用基礎設施去建立，不在應用程式建立 topic
 */
@Configuration
public class KafkaConfig {

    /** 成交流水（對外發布） */
    @Bean
    NewTopic tradeTopic() {
        return new NewTopic("trade.executed", 3, (short) 1);
    }

    /** 事件儲存（可做 compacted topic 或落 DB） */
    @Bean
    NewTopic eventStoreTopic() {
        return new NewTopic("event.store.trade", 3, (short) 1);
    }
}
