package com.example.exchange.interfaces.consumer;

import com.example.exchange.domain.event.TradeExecuted;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * 成交事件消費者（示範）
 * - 用於對外推播、審計、指標統計等副作用
 */
@Component
public class TradeEventConsumer {

    @KafkaListener(topics = "trade.executed", groupId = "trade-consumer")
    public void onTrade(TradeExecuted e) {
        // TODO: 推播到 WebSocket、記錄審計、指標上報…
    }
}
