/*
 * 檔案用途：基礎設施設定，建立 Spring Bean 並連接 Kafka、Redis、Web3j 或 HTTP client。
 */
package com.example.exchange.infra.config;

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

    /** 訂單生命週期事件，用於審計、查詢 projection 與使用者推送 */
    @Bean
    NewTopic orderLifecycleTopic() {
        return new NewTopic("order.lifecycle", 3, (short) 1);
    }

    /** 事件儲存（可做 compacted topic 或落 DB） */
    @Bean
    NewTopic eventStoreTopic() {
        return new NewTopic("event.store.trade", 3, (short) 1);
    }

    @Bean
    NewTopic fundingSettledTopic() {
        return new NewTopic("funding.settled", 3, (short) 1);
    }

    @Bean
    NewTopic positionLiquidatedTopic() {
        return new NewTopic("position.liquidated", 3, (short) 1);
    }

    @Bean
    NewTopic domainEventsTopic() {
        return new NewTopic("domain.events", 3, (short) 1);
    }

    /** Polymarket user channel 私有 order/trade/settlement lifecycle */
    @Bean
    NewTopic polymarketUserEventsTopic() {
        return new NewTopic("polymarket.user.events", 3, (short) 1);
    }
}
