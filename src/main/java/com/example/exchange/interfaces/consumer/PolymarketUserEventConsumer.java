/*
 * 檔案用途：事件 Consumer，接收 Kafka 訊息並銜接後續業務處理。
 */
package com.example.exchange.interfaces.consumer;

import com.example.exchange.domain.model.dto.PolymarketUserWsEvent;
import com.example.exchange.domain.service.PolymarketUserEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PolymarketUserEventConsumer {

    private final PolymarketUserEventService polymarketUserEventService;

    @KafkaListener(
            topics = "polymarket.user.events",
            groupId = "polymarket-user-event-consumer"
    )
    public void onEvent(PolymarketUserWsEvent event) {
        polymarketUserEventService.handle(event);
    }
}
