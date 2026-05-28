/*
 * 檔案用途：撮合基礎設施，提供 in-memory matching event log baseline。
 */
package com.example.exchange.infra.matching;

import com.example.exchange.domain.event.TradeExecuted;
import com.example.exchange.domain.model.dto.MatchingEventLogEntry;
import com.example.exchange.domain.repository.MatchingEventLog;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory matching event log adapter。
 *
 * <p>用於先固定 event offset 與 validation 行為；production 後應替換為 durable
 * event storage，避免 process restart 後遺失成交事件 checkpoint。</p>
 */
public class InMemoryMatchingEventLog implements MatchingEventLog {

    private final Map<String, List<MatchingEventLogEntry>> entries = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> offsets = new ConcurrentHashMap<>();

    @Override
    public MatchingEventLogEntry append(String symbolCode, long commandOffset, TradeExecuted trade) {
        return append(symbolCode, commandOffset, trade, null, 0L);
    }

    @Override
    public MatchingEventLogEntry append(
            String symbolCode,
            long commandOffset,
            TradeExecuted trade,
            String ownerId,
            long ownerEpoch
    ) {
        String symbol = normalize(symbolCode);
        long offset = offsets.computeIfAbsent(symbol, ignored -> new AtomicLong()).incrementAndGet();
        MatchingEventLogEntry entry = new MatchingEventLogEntry(
                symbol,
                offset,
                commandOffset,
                trade,
                ownerId,
                Math.max(0L, ownerEpoch),
                Instant.now()
        );
        entries.computeIfAbsent(symbol, ignored -> new ArrayList<>()).add(entry);
        return entry;
    }

    @Override
    public List<MatchingEventLogEntry> listAfter(String symbolCode, long offset) {
        return listAll(symbolCode).stream()
                .filter(entry -> entry.offset() > offset)
                .sorted(Comparator.comparingLong(MatchingEventLogEntry::offset))
                .toList();
    }

    @Override
    public List<MatchingEventLogEntry> listAll(String symbolCode) {
        return List.copyOf(entries.getOrDefault(normalize(symbolCode), List.of()));
    }

    @Override
    public long lastOffset(String symbolCode) {
        AtomicLong offset = offsets.get(normalize(symbolCode));
        return offset == null ? 0L : offset.get();
    }

    private static String normalize(String symbolCode) {
        return symbolCode == null ? "" : symbolCode.trim().toUpperCase();
    }
}
