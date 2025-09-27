package com.example.java21_OLAP.application.service;

import com.example.java21_OLAP.domain.model.*;
import com.example.java21_OLAP.domain.repository.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 快照恢復服務（簡化骨架）
 * - 讀最新快照 -> 還原聚合根 -> 從 lastSeq 之後回放事件
 * - 回放事件的取得與套用邏輯，實務上由 infra EventStore 或專屬 replay 程序提供
 */
@Service
public class RecoveryService {

    private final SnapshotRepository snapshotRepo;
    private final EventStore eventStore;
    private final AccountRepository accountRepo;
    private final PositionRepository positionRepo;

    public RecoveryService(SnapshotRepository snapshotRepo,
                           EventStore eventStore,
                           AccountRepository accountRepo,
                           PositionRepository positionRepo) {
        this.snapshotRepo = snapshotRepo;
        this.eventStore = eventStore;
        this.accountRepo = accountRepo;
        this.positionRepo = positionRepo;
    }

    /** 依 uid 恢復狀態（示範版） */
    @SuppressWarnings("unchecked")
    public void recover(long uid) {
        var snapOpt = snapshotRepo.latest(uid);
        if (snapOpt.isEmpty()) return;

        Snapshot snap = snapOpt.get();

        // 1) 還原 Account（示範：只建立空帳戶；實務上應從 snapshot.sh 內容重建所有欄位）
        Map<String, Object> accMap = (Map<String, Object>) snap.aggregates().get("account");
        if (accMap != null) {
            Account acc = new Account(uid);
            accountRepo.save(acc);
        }

        // 2) 還原 Positions
        List<Map<String, Object>> posList =
                (List<Map<String, Object>>) snap.aggregates().getOrDefault("positions", List.of());

        for (Map<String, Object> pm : posList) {
            Symbol sym = new Symbol(
                    (String) pm.get("base"),
                    (String) pm.get("quote"),
                    ((Number) pm.get("priceScale")).intValue(),
                    ((Number) pm.get("qtyScale")).intValue()
            );
            MarginMode mode = MarginMode.valueOf((String) pm.get("mode"));
            var levNum = (Number) pm.get("lev");
            var lev = (levNum == null) ? 20 : levNum.intValue();

            Position p = new Position(uid, sym, mode, java.math.BigDecimal.valueOf(lev));
            positionRepo.save(p);
        }

        long lastSeq = snap.lastEventSeq();

        // 3) 從 lastSeq 之後回放成交事件（此處省略事件拉取與作用的細節）
        //    - 實作可在 infra 內提供方法：List<TradeExecuted> fetchAfter(uid, lastSeq)
        //    - 逐筆 apply 到 Position，並確保以 seq 去重
    }
}
