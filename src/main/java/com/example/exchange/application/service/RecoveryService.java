package com.example.exchange.application.service;

import com.example.exchange.domain.event.TradeExecuted;
import com.example.exchange.domain.model.dto.RecoveryResult;
import com.example.exchange.domain.model.dto.Snapshot;
import com.example.exchange.domain.model.entity.Account;
import com.example.exchange.domain.model.entity.Position;
import com.example.exchange.domain.model.entity.Symbol;
import com.example.exchange.domain.model.enums.MarginMode;
import com.example.exchange.domain.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 快照恢復服務
 * - 讀最新快照 -> 還原聚合根 -> 從 lastSeq 之後回放事件
 */
@Service
@RequiredArgsConstructor
public class RecoveryService {

    private final SnapshotRepository snapshotRepo;
    private final EventStore eventStore;
    private final AccountRepository accountRepo;
    private final PositionRepository positionRepo;

    /** 依 uid 恢復狀態（示範版） */
    @SuppressWarnings("unchecked")
    public RecoveryResult recover(long uid, Long fromSeq) {
        var snapOpt = snapshotRepo.latest(uid);
        if (snapOpt.isEmpty()) {
            return new RecoveryResult(uid, false, 0, fromSeq == null ? 0 : fromSeq, 0, Instant.now());
        }

        Snapshot snap = snapOpt.get();

        // 1) 還原 Account
        Map<String, Object> accMap = (Map<String, Object>) snap.aggregates().get("account");
        if (accMap != null) {
            Account acc = new Account(uid);
            acc.restoreCross(
                    toBigDecimal(accMap.get("crossBalance")),
                    toBigDecimal(accMap.get("crossAvailable")),
                    toBigDecimal(accMap.get("crossOrderHold")),
                    toBigDecimal(accMap.get("crossPositionMargin"))
            );
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

            positionRepo.save(
                    Position.builder()
                            .uid(uid)
                            .symbol(sym)
                            .mode(mode)
                            .leverage(levNum == null ? BigDecimal.valueOf(20) : toBigDecimal(levNum))
                            .qty(toBigDecimal(pm.get("qty")))
                            .entryPrice(toBigDecimal(pm.get("entryPrice")))
                            .margin(toBigDecimal(pm.get("margin")))
                            .realizedPnl(toBigDecimal(pm.get("realizedPnl")))
                            .feePaid(toBigDecimal(pm.get("feePaid")))
                            .rebateEarned(toBigDecimal(pm.get("rebateEarned")))
                            .fundingPaid(toBigDecimal(pm.get("fundingPaid")))
                            .fundingReceived(toBigDecimal(pm.get("fundingReceived")))
                            .build());
        }

        long replayFromSeq = fromSeq == null ? snap.lastEventSeq() : Math.max(0, fromSeq);
        List<TradeExecuted> events = eventStore.fetchAfter(uid, replayFromSeq, 10_000);
        for (TradeExecuted event : events) {
            Position position = positionRepo.find(uid, event.symbol()).orElseGet(() ->
                    Position.builder()
                            .uid(uid)
                            .symbol(event.symbol())
                            .mode(MarginMode.CROSS)
                            .leverage(BigDecimal.valueOf(20))
                            .build()
            );
            position.applyTradeWithPnl(event.qty(), event.price());
            positionRepo.save(position);
        }

        return new RecoveryResult(uid, true, snap.lastEventSeq(), replayFromSeq, events.size(), Instant.now());
    }

    public RecoveryResult recover(long uid) {
        return recover(uid, null);
    }

    private static BigDecimal toBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal decimal) return decimal;
        if (value instanceof Number number) return BigDecimal.valueOf(number.doubleValue());
        return new BigDecimal(String.valueOf(value));
    }
}
