package com.example.exchange.application.scheduler;

import com.example.exchange.application.service.OutboxService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class OutboxRelayScheduler {

    private final OutboxService outboxService;
    private final KafkaTemplate<String, Object> kafka;

//    @Scheduled(fixedDelay = 10_000)
    public void relay() {
        outboxService.relayDue(100, (topic, key, payload) ->
                kafka.send(topic, key, payload).get(3, TimeUnit.SECONDS));
    }
}
