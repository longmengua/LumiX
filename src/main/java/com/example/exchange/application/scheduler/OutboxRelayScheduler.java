/*
 * 檔案用途：應用層排程任務，定期驅動快照、資金費、對帳或 Polymarket 同步。
 */
package com.example.exchange.application.scheduler;

import com.example.exchange.application.service.OutboxService;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class OutboxRelayScheduler {

    private final OutboxService outboxService;
    private final KafkaTemplate<String, Object> kafka;

//    @Scheduled(fixedDelay = 10_000)
    public void relay() {
        outboxService.relayDue(100, this::send);
    }

    private void send(String topic, String key, Object payload, Map<String, String> headers) throws Exception {
        ProducerRecord<String, Object> record = new ProducerRecord<>(topic, key, payload);
        if (headers != null) {
            headers.forEach((name, value) -> {
                if (name == null || name.isBlank() || value == null || value.isBlank()) return;
                record.headers().add(name, value.getBytes(StandardCharsets.UTF_8));
            });
        }
        kafka.send(record).get(3, TimeUnit.SECONDS);
    }
}
