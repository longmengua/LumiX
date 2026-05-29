/*
 * 檔案用途：應用服務，管理 matching worker 啟動時的 lease acquire 與 recovery readiness。
 */
package com.example.exchange.application.service;

import com.example.exchange.domain.model.dto.MatchingRecoveryResult;
import com.example.exchange.domain.model.dto.MatchingSequencerLease;
import com.example.exchange.domain.repository.MatchingCommandLog;
import com.example.exchange.domain.repository.MatchingEventLog;
import com.example.exchange.infra.config.MatchingWorkerProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Matching worker lifecycle baseline.
 *
 * <p>此服務把 production worker startup 的順序固定成：讀取設定 symbols、取得 per-symbol
 * lease、執行 recovery、保存 owner/epoch readiness。實際 command intake 可用
 * `ownerContext(symbol)` 取得目前 worker 對該 symbol 的 owner/epoch，再呼叫
 * `MatchingWorkerExecutionService`。</p>
 */
@Service
@RequiredArgsConstructor
public class MatchingWorkerLifecycleService {

    private final MatchingWorkerProperties properties;
    private final MatchingSequencerLeaseService leaseService;
    private final MatchingRecoveryService recoveryService;
    private final MatchingCommandLog commandLog;
    private final MatchingEventLog eventLog;
    private final Map<String, MatchingWorkerOwnerContext> ownerContexts = new ConcurrentHashMap<>();

    public List<MatchingWorkerStartupResult> startConfiguredSymbols() {
        if (!properties.isEnabled()) {
            return List.of();
        }
        List<MatchingWorkerStartupResult> results = new ArrayList<>();
        for (String symbol : properties.getSymbols()) {
            if (symbol == null || symbol.isBlank()) {
                continue;
            }
            results.add(startSymbol(symbol));
        }
        return List.copyOf(results);
    }

    public MatchingWorkerStartupResult startSymbol(String symbolCode) {
        String symbol = normalize(symbolCode);
        MatchingSequencerLease lease = leaseService.acquire(
                        symbol,
                        properties.getOwnerId(),
                        Duration.ofMillis(properties.getLeaseTtlMs())
                )
                .orElseThrow(() -> new IllegalStateException("matching worker lease acquire failed: " + symbol));
        MatchingRecoveryResult recovery = recoveryService.recoverSymbol(symbol);
        if (!recovery.validationValid()) {
            throw new IllegalStateException("matching worker recovery validation failed: " + symbol);
        }
        MatchingWorkerOwnerContext context = new MatchingWorkerOwnerContext(
                symbol,
                lease.ownerId(),
                lease.epoch(),
                lease.expiresAt(),
                recovery.recoveredCommandOffset(),
                eventLog.lastOffset(symbol),
                recovery.recoveredAt()
        );
        ownerContexts.put(symbol, context);
        return new MatchingWorkerStartupResult(symbol, context, recovery);
    }

    public Optional<MatchingWorkerOwnerContext> ownerContext(String symbolCode) {
        return Optional.ofNullable(ownerContexts.get(normalize(symbolCode)));
    }

    /**
     * 回傳 production routing 可用的 owner context；cutover fence 開啟且 symbol 未 ready 時拒絕 fallback。
     */
    public Optional<MatchingWorkerOwnerContext> routingOwnerContext(String symbolCode) {
        String symbol = normalize(symbolCode);
        Optional<MatchingWorkerOwnerContext> context = ownerContext(symbol);
        if (context.isPresent()) {
            return context;
        }
        if (properties != null
                && properties.isEnabled()
                && properties.isFenceLegacyRouting()
                && configuredForWorker(symbol)) {
            throw new IllegalStateException("matching worker owner context is not ready: " + symbol);
        }
        return Optional.empty();
    }

    /**
     * 判斷 symbol 是否已完成 startup recovery 且仍由目前 worker context 管理。
     */
    public boolean isReady(String symbolCode) {
        return ownerContext(symbolCode).isPresent();
    }

    /**
     * 對所有已 ready 的 symbol 續租，並用目前 command/event log offset 更新 lease checkpoint。
     */
    public List<MatchingWorkerOwnerContext> renewOwnedSymbols() {
        List<MatchingWorkerOwnerContext> renewed = new ArrayList<>();
        for (String symbol : new ArrayList<>(ownerContexts.keySet())) {
            renewSymbol(symbol).ifPresent(renewed::add);
        }
        return List.copyOf(renewed);
    }

    /**
     * 對單一 symbol 續租；若 lease store 拒絕續租，移除 readiness，避免 stale owner 繼續接 command。
     */
    public Optional<MatchingWorkerOwnerContext> renewSymbol(String symbolCode) {
        String symbol = normalize(symbolCode);
        MatchingWorkerOwnerContext context = ownerContexts.get(symbol);
        if (context == null) {
            return Optional.empty();
        }
        long commandOffset = commandLog.lastOffset(symbol);
        long eventOffset = eventLog.lastOffset(symbol);
        Optional<MatchingSequencerLease> renewed = leaseService.renew(
                symbol,
                context.ownerId(),
                context.ownerEpoch(),
                Duration.ofMillis(properties.getLeaseTtlMs()),
                commandOffset,
                eventOffset
        );
        if (renewed.isEmpty()) {
            ownerContexts.remove(symbol);
            return Optional.empty();
        }
        MatchingSequencerLease lease = renewed.get();
        MatchingWorkerOwnerContext refreshed = new MatchingWorkerOwnerContext(
                symbol,
                lease.ownerId(),
                lease.epoch(),
                lease.expiresAt(),
                commandOffset,
                eventOffset,
                context.readyAt()
        );
        ownerContexts.put(symbol, refreshed);
        return Optional.of(refreshed);
    }

    public Map<String, MatchingWorkerOwnerContext> ownerContexts() {
        return Map.copyOf(new LinkedHashMap<>(ownerContexts));
    }

    private static String normalize(String symbolCode) {
        return symbolCode == null ? "" : symbolCode.trim().toUpperCase();
    }

    private boolean configuredForWorker(String symbol) {
        return properties.getSymbols().stream()
                .filter(configured -> configured != null && !configured.isBlank())
                .map(MatchingWorkerLifecycleService::normalize)
                .anyMatch(symbol::equals);
    }

    public record MatchingWorkerOwnerContext(
            String symbolCode,
            String ownerId,
            long ownerEpoch,
            Instant leaseExpiresAt,
            long recoveredCommandOffset,
            long recoveredEventOffset,
            Instant readyAt
    ) {
    }

    public record MatchingWorkerStartupResult(
            String symbolCode,
            MatchingWorkerOwnerContext ownerContext,
            MatchingRecoveryResult recoveryResult
    ) {
    }
}
