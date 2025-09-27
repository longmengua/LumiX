package com.example.exchange.infra.kafka;

import com.example.exchange.domain.event.TradeExecuted;
import com.example.exchange.domain.repository.EventStore;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Repository;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 事件儲存（Kafka 版簡化骨架）
 * - 以 AtomicLong 模擬 seq（生產請改為由 Kafka/DB 提供單調序號）
 * - 事件實際 payload 送到 "event.store.trade"（可做 compacted topic + key=uid）
 */
@Repository
public class KafkaEventStore implements EventStore {

    private final KafkaTemplate<String, Object> kafka;
    private final AtomicLong seq = new AtomicLong(); // DEMO：請替換為真正的序號來源

    public KafkaEventStore(KafkaTemplate<String, Object> kafka) {
        this.kafka = kafka;
    }

    @Override
    public long append(TradeExecuted e) {
        long s = seq.incrementAndGet();
        kafka.send("event.store.trade", e.withSeq(s));
        return s;
    }

    @Override
    public long lastSeq(long uid) {
        // DEMO：僅回傳目前計數；實務請查詢該 uid 的最新 seq
        return seq.get();
    }
}
