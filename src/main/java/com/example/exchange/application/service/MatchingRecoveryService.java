/*
 * 檔案用途：應用服務，編排 matching snapshot、command log replay 與 validation report 持久化。
 */
package com.example.exchange.application.service;

import com.example.exchange.domain.model.dto.Symbol;

import com.example.exchange.domain.model.dto.MatchingCommandLogEntry;
import com.example.exchange.domain.model.dto.MatchingEngineSnapshot;
import com.example.exchange.domain.model.dto.MatchingRecoveryResult;
import com.example.exchange.domain.model.dto.MatchingReplayValidationReport;
import com.example.exchange.domain.repository.MatchingCommandLog;
import com.example.exchange.domain.repository.MatchingReplayValidationReportStore;
import com.example.exchange.domain.repository.MatchingSnapshotStore;
import com.example.exchange.domain.service.MatchingEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * Symbol-level matching recovery orchestration。
 *
 * <p>此服務是 worker startup / takeover 的明確入口：讀最新 snapshot，從 command log
 * replay 後續指令，匯出新的 checkpoint，執行 deterministic validation，最後保存
 * snapshot 與 validation report。</p>
 */
@Service
@RequiredArgsConstructor
public class MatchingRecoveryService {

    private final MatchingEngine matchingEngine;
    private final MatchingCommandLog commandLog;
    private final MatchingSnapshotStore snapshotStore;
    private final MatchingReplayValidationReportStore reportStore;

    /**
     * 恢復單一 symbol 的 matching 狀態。
     *
     * <p>此方法會寫入 matching engine 狀態、保存新的 durable snapshot，並保存 replay validation report；
     * command/event log 不會被重寫，replay 只套用 snapshot command offset 之後的 command。</p>
     */
    public MatchingRecoveryResult recoverSymbol(String symbolCode) {
        String symbol = requireSymbol(symbolCode);
        var latestSnapshot = snapshotStore.latest(symbol);
        MatchingEngineSnapshot startSnapshot = latestSnapshot.orElseGet(() -> emptySnapshot(symbol));
        List<MatchingCommandLogEntry> commands = commandLog.listAll(symbol);
        int replayedCommands = (int) commands.stream()
                .filter(command -> command.offset() > startSnapshot.commandOffset())
                .count();

        matchingEngine.replay(startSnapshot, commands);
        MatchingEngineSnapshot recoveredSnapshot = matchingEngine.exportSnapshot(symbol);
        MatchingReplayValidationReport report =
                matchingEngine.validateReplay(startSnapshot, commands, recoveredSnapshot);

        snapshotStore.save(recoveredSnapshot);
        reportStore.save(report);

        return new MatchingRecoveryResult(
                symbol,
                true,
                latestSnapshot.isPresent(),
                startSnapshot.commandOffset(),
                recoveredSnapshot.commandOffset(),
                replayedCommands,
                report.valid(),
                report.issues(),
                Instant.now()
        );
    }

    private static MatchingEngineSnapshot emptySnapshot(String symbolCode) {
        return new MatchingEngineSnapshot(
                symbolCode,
                0L,
                0L,
                0L,
                List.of(),
                List.of(),
                Instant.now()
        );
    }

    private static String normalize(String symbolCode) {
        return symbolCode == null ? "" : symbolCode.trim().toUpperCase();
    }

    private static String requireSymbol(String symbolCode) {
        String symbol = normalize(symbolCode);
        if (symbol.isBlank()) {
            throw new IllegalArgumentException("matching recovery symbol is required");
        }
        return symbol;
    }
}
