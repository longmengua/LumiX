package com.example.exchange.application.scheduler;

import com.example.exchange.domain.model.Snapshot;
import com.example.exchange.domain.repository.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 快照排程（示範）
 * - 實務：以事件數閾值或按 UID sharding，避免掃描全量
 */
@Component
public class SnapshotScheduler {

    private final SnapshotRepository snapRepo;
    private final AccountRepository accRepo;
    private final PositionRepository posRepo;
    private final EventStore eventStore;

    public SnapshotScheduler(SnapshotRepository snapRepo,
                             AccountRepository accRepo,
                             PositionRepository posRepo,
                             EventStore eventStore) {
        this.snapRepo = snapRepo;
        this.accRepo = accRepo;
        this.posRepo = posRepo;
        this.eventStore = eventStore;
    }

    /** 每 1 分鐘做示範性快照（實務請改成事件驅動或分片輪詢） */
//    @Scheduled(fixedDelay = 60_000)
    public void run() {
        // DEMO：假設掃描少量 uid；生產要改為 key-scan/shard 分批
        for (long uid : List.of(1L, 2L)) {
            accRepo.findByUid(uid).ifPresent(acc -> {
                Map<String, Object> map = new HashMap<>();
                map.put("account", Map.of("uid", acc.uid()));

                var positions = posRepo.findAllByUid(uid).stream().map(p -> Map.of(
                        "base", p.getSymbol().getBase(),
                        "quote", p.getSymbol().getQuote(),
                        "priceScale", p.getSymbol().getPriceScale(),
                        "qtyScale", p.getSymbol().getQtyScale(),
                        "mode", p.getMode().name(),
                        "lev", p.getLeverage()
                )).toList();

                map.put("positions", positions);

                long lastSeq = eventStore.lastSeq(uid);
                snapRepo.save(new Snapshot(uid, map, Instant.now(), lastSeq));
            });
        }
    }
}
