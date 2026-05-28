/*
 * 檔案用途：測試 matching recovery orchestration 的 snapshot replay 與 validation report 保存。
 */
package com.example.exchange.application.service;

import com.example.exchange.domain.model.dto.MatchingEngineSnapshot;
import com.example.exchange.domain.model.dto.MatchingRecoveryResult;
import com.example.exchange.domain.model.dto.MatchingReplayValidationReport;
import com.example.exchange.domain.model.entity.Order;
import com.example.exchange.domain.model.entity.Symbol;
import com.example.exchange.domain.model.enums.OrderSide;
import com.example.exchange.domain.model.enums.OrderType;
import com.example.exchange.domain.repository.MatchingReplayValidationReportStore;
import com.example.exchange.domain.repository.MatchingSnapshotStore;
import com.example.exchange.infra.matching.InMemoryMatchingCommandLog;
import com.example.exchange.infra.matching.InMemoryMatchingEngine;
import com.example.exchange.infra.matching.InMemoryMatchingEventLog;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MatchingRecoveryService tests。
 *
 * <p>用 shared command log 模擬 durable command storage，再用新的 engine 代表 worker takeover
 * 後的空狀態，確認 service 能從 snapshot checkpoint 重建 book。</p>
 */
class MatchingRecoveryServiceTest {

    private final Symbol symbol = Symbol.builder()
            .base("BTC")
            .quote("USDT")
            .priceScale(2)
            .qtyScale(3)
            .build();

    @Test
    @DisplayName("recoverSymbol 會從 durable snapshot 與 command log 重建撮合狀態並保存 validation report")
    /**
     * 流程：原 engine 先保存 checkpoint snapshot -> 後續 command 形成成交 ->
     * 新 engine 透過 recovery service replay -> 驗證 snapshot/report 都被保存且 book 狀態一致。
     */
    void recoverSymbolReplaysCommandsAfterSnapshotAndPersistsValidationReport() {
        InMemoryMatchingCommandLog commandLog = new InMemoryMatchingCommandLog();
        InMemoryMatchingEngine sourceEngine = new InMemoryMatchingEngine(commandLog, new InMemoryMatchingEventLog());
        InMemoryMatchingSnapshotStore snapshotStore = new InMemoryMatchingSnapshotStore();
        InMemoryMatchingReplayReportStore reportStore = new InMemoryMatchingReplayReportStore();

        sourceEngine.submit(limit(1, OrderSide.SELL, "100", "1"));
        MatchingEngineSnapshot checkpoint = sourceEngine.exportSnapshot("BTCUSDT");
        snapshotStore.save(checkpoint);
        sourceEngine.submit(limit(2, OrderSide.SELL, "101", "1"));
        sourceEngine.submit(limit(3, OrderSide.BUY, "101", "2"));
        MatchingEngineSnapshot expected = sourceEngine.exportSnapshot("BTCUSDT");

        InMemoryMatchingEngine recoveredEngine = new InMemoryMatchingEngine(commandLog, new InMemoryMatchingEventLog());
        MatchingRecoveryService service = new MatchingRecoveryService(
                recoveredEngine,
                commandLog,
                snapshotStore,
                reportStore
        );

        MatchingRecoveryResult result = service.recoverSymbol("btcusdt");

        assertThat(result.recovered()).isTrue();
        assertThat(result.snapshotFound()).isTrue();
        assertThat(result.snapshotCommandOffset()).isEqualTo(checkpoint.commandOffset());
        assertThat(result.recoveredCommandOffset()).isEqualTo(expected.commandOffset());
        assertThat(result.replayedCommands()).isEqualTo(2);
        assertThat(result.validationValid()).isTrue();
        assertThat(result.validationIssues()).isEmpty();
        assertThat(snapshotStore.latest("BTCUSDT")).get()
                .extracting(MatchingEngineSnapshot::commandOffset, MatchingEngineSnapshot::eventOffset)
                .containsExactly(expected.commandOffset(), expected.eventOffset());
        assertThat(reportStore.findBySymbol("BTCUSDT", 10)).hasSize(1)
                .first()
                .extracting(MatchingReplayValidationReport::valid)
                .isEqualTo(true);
        assertThat(recoveredEngine.snapshot("BTCUSDT", 10).asks()).isEmpty();
        sourceEngine.shutdown();
        recoveredEngine.shutdown();
    }

    @Test
    @DisplayName("recoverSymbol 無既有 snapshot 時會從空 checkpoint replay 全量 command log")
    /**
     * 流程：只保留 command log、不保存 snapshot -> 新 engine recovery ->
     * 驗證 service 使用空 snapshot replay 全量 command 並建立新的 durable snapshot。
     */
    void recoverSymbolReplaysAllCommandsWhenSnapshotIsMissing() {
        InMemoryMatchingCommandLog commandLog = new InMemoryMatchingCommandLog();
        InMemoryMatchingEngine sourceEngine = new InMemoryMatchingEngine(commandLog, new InMemoryMatchingEventLog());
        InMemoryMatchingSnapshotStore snapshotStore = new InMemoryMatchingSnapshotStore();
        InMemoryMatchingReplayReportStore reportStore = new InMemoryMatchingReplayReportStore();
        sourceEngine.submit(limit(1, OrderSide.SELL, "100", "1"));
        sourceEngine.submit(limit(2, OrderSide.BUY, "100", "1"));
        MatchingEngineSnapshot expected = sourceEngine.exportSnapshot("BTCUSDT");

        InMemoryMatchingEngine recoveredEngine = new InMemoryMatchingEngine(commandLog, new InMemoryMatchingEventLog());
        MatchingRecoveryService service = new MatchingRecoveryService(
                recoveredEngine,
                commandLog,
                snapshotStore,
                reportStore
        );

        MatchingRecoveryResult result = service.recoverSymbol("BTCUSDT");

        assertThat(result.recovered()).isTrue();
        assertThat(result.snapshotFound()).isFalse();
        assertThat(result.snapshotCommandOffset()).isZero();
        assertThat(result.replayedCommands()).isEqualTo(2);
        assertThat(snapshotStore.latest("BTCUSDT")).get()
                .extracting(MatchingEngineSnapshot::commandOffset, MatchingEngineSnapshot::eventOffset)
                .containsExactly(expected.commandOffset(), expected.eventOffset());
        assertThat(reportStore.findBySymbol("BTCUSDT", 10)).hasSize(1)
                .first()
                .extracting(MatchingReplayValidationReport::valid)
                .isEqualTo(true);
        sourceEngine.shutdown();
        recoveredEngine.shutdown();
    }

    /**
     * 建立 LIMIT 測試訂單，讓 recovery 測試只關心 command replay 與 checkpoint 行為。
     */
    private Order limit(long uid, OrderSide side, String price, String qty) {
        return Order.builder()
                .uid(uid)
                .symbol(symbol)
                .side(side)
                .type(OrderType.LIMIT)
                .price(new BigDecimal(price))
                .qty(new BigDecimal(qty))
                .origQty(new BigDecimal(qty))
                .build();
    }

    private static final class InMemoryMatchingSnapshotStore implements MatchingSnapshotStore {
        private final Map<String, MatchingEngineSnapshot> snapshots = new ConcurrentHashMap<>();

        @Override
        public void save(MatchingEngineSnapshot snapshot) {
            snapshots.put(normalize(snapshot.symbolCode()), snapshot);
        }

        @Override
        public Optional<MatchingEngineSnapshot> latest(String symbolCode) {
            return Optional.ofNullable(snapshots.get(normalize(symbolCode)));
        }
    }

    private static final class InMemoryMatchingReplayReportStore implements MatchingReplayValidationReportStore {
        private final Map<String, List<MatchingReplayValidationReport>> reports = new ConcurrentHashMap<>();

        @Override
        public void save(MatchingReplayValidationReport report) {
            reports.computeIfAbsent(normalize(report.symbolCode()), ignored -> new ArrayList<>()).add(report);
        }

        @Override
        public List<MatchingReplayValidationReport> findBySymbol(String symbolCode, int limit) {
            return reports.getOrDefault(normalize(symbolCode), List.of()).stream()
                    .sorted(Comparator.comparing(MatchingReplayValidationReport::validatedAt).reversed())
                    .limit(Math.max(1, limit))
                    .toList();
        }
    }

    private static String normalize(String symbolCode) {
        return symbolCode == null ? "" : symbolCode.trim().toUpperCase();
    }
}
