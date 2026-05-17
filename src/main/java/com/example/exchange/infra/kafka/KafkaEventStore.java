package com.example.exchange.infra.kafka;

import com.example.exchange.domain.event.TradeExecuted;
import com.example.exchange.domain.repository.EventStore;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
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
    private final Map<Long, CopyOnWriteArrayList<TradeExecuted>> replayIndex = new ConcurrentHashMap<>();

    public KafkaEventStore(KafkaTemplate<String, Object> kafka) {
        this.kafka = kafka;
    }

    @Override
    public long append(TradeExecuted e) {
        long s = seq.incrementAndGet();
        TradeExecuted withSeq = e.withSeq(s);
        replayIndex.computeIfAbsent(withSeq.uid(), ignored -> new CopyOnWriteArrayList<>()).add(withSeq);
        kafka.send("event.store.trade", withSeq.symbol().code(), withSeq);
        return s;
    }

    @Override
    public long lastSeq(long uid) {
        return replayIndex.getOrDefault(uid, new CopyOnWriteArrayList<>()).stream()
                .mapToLong(TradeExecuted::seq)
                .max()
                .orElse(0L);
    }

    @Override
    public List<TradeExecuted> fetchAfter(long uid, long afterSeq, int limit) {
        return replayIndex.getOrDefault(uid, new CopyOnWriteArrayList<>()).stream()
                .filter(event -> event.seq() > afterSeq)
                .sorted(Comparator.comparingLong(TradeExecuted::seq))
                .limit(Math.max(1, limit))
                .toList();
    }
}
