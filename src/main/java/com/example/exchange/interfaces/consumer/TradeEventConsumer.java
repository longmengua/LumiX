/*
 * 檔案用途：事件 Consumer，接收 Kafka 訊息並銜接後續業務處理。
 */
package com.example.exchange.interfaces.consumer;

import com.example.exchange.application.service.PushGatewayService;
import com.example.exchange.domain.event.TradeExecuted;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * 成交事件消費者（示範）
 * - 用於對外推播、審計、指標統計等副作用
 */
@Component
@RequiredArgsConstructor
public class TradeEventConsumer {

    private final PushGatewayService pushGatewayService;

    @KafkaListener(topics = "trade.executed", groupId = "trade-consumer")
    public void onTrade(TradeExecuted e) {
        pushGatewayService.publishMarket(e.symbol().code(), "trade", e);
        pushGatewayService.publishUser(e.uid(), "trade", e);
    }
}
