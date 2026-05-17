/*
 * 檔案用途：應用層排程任務，定期驅動快照、資金費、對帳或 Polymarket 同步。
 */
package com.example.exchange.application.scheduler;

import com.example.exchange.domain.model.dto.Snapshot;
import com.example.exchange.domain.repository.*;
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
                map.put("account", Map.of(
                        "uid", acc.uid(),
                        "crossBalance", acc.crossBalance(),
                        "crossAvailable", acc.crossAvailable(),
                        "crossOrderHold", acc.crossOrderHold(),
                        "crossPositionMargin", acc.crossPositionMargin()
                ));

                var positions = posRepo.findAllByUid(uid).stream().map(p -> {
                    Map<String, Object> position = new HashMap<>();
                    position.put("base", p.getSymbol().getBase());
                    position.put("quote", p.getSymbol().getQuote());
                    position.put("priceScale", p.getSymbol().getPriceScale());
                    position.put("qtyScale", p.getSymbol().getQtyScale());
                    position.put("mode", p.getMode().name());
                    position.put("lev", p.getLeverage());
                    position.put("qty", p.getQty());
                    position.put("entryPrice", p.getEntryPrice());
                    position.put("margin", p.getMargin());
                    position.put("realizedPnl", p.getRealizedPnl());
                    position.put("feePaid", p.getFeePaid());
                    position.put("rebateEarned", p.getRebateEarned());
                    position.put("fundingPaid", p.getFundingPaid());
                    position.put("fundingReceived", p.getFundingReceived());
                    return position;
                }).toList();

                map.put("positions", positions);

                long lastSeq = eventStore.lastSeq(uid);
                snapRepo.save(new Snapshot(uid, map, Instant.now(), lastSeq));
            });
        }
    }
}
