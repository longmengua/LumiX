/*
 * 檔案用途：撮合基礎設施，提供 in-memory matching command log baseline。
 */
package com.example.exchange.infra.matching;

import com.example.exchange.domain.model.dto.MatchingCommandLogEntry;
import com.example.exchange.domain.model.dto.Order;
import com.example.exchange.domain.model.enums.MatchingCommandType;
import com.example.exchange.domain.repository.MatchingCommandLog;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory command log adapter。
 *
 * <p>此類別不是 production durable storage；它提供 per-symbol offset 與 replay
 * query baseline，讓 matching replay 行為可以先被 deterministic tests 固定。</p>
 */
public class InMemoryMatchingCommandLog implements MatchingCommandLog {

    private final Map<String, List<MatchingCommandLogEntry>> entries = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> offsets = new ConcurrentHashMap<>();

    @Override
    public MatchingCommandLogEntry append(
            String symbolCode,
            MatchingCommandType type,
            Order order,
            BigDecimal newPrice,
            BigDecimal newQty
    ) {
        return append(symbolCode, type, order, newPrice, newQty, null, 0L);
    }

    @Override
    public MatchingCommandLogEntry append(
            String symbolCode,
            MatchingCommandType type,
            Order order,
            BigDecimal newPrice,
            BigDecimal newQty,
            String ownerId,
            long ownerEpoch
    ) {
        String symbol = normalize(symbolCode);
        long offset = offsets.computeIfAbsent(symbol, ignored -> new AtomicLong()).incrementAndGet();
        MatchingCommandLogEntry entry = new MatchingCommandLogEntry(
                symbol,
                offset,
                type,
                order,
                null,
                newPrice,
                newQty,
                ownerId,
                Math.max(0L, ownerEpoch),
                Instant.now()
        );
        entries.computeIfAbsent(symbol, ignored -> new ArrayList<>()).add(entry);
        return entry;
    }

    @Override
    public MatchingCommandLogEntry appendCancelReplace(String symbolCode, Order originalOrder, Order replacementOrder) {
        return appendCancelReplace(symbolCode, originalOrder, replacementOrder, null, 0L);
    }

    @Override
    public MatchingCommandLogEntry appendCancelReplace(
            String symbolCode,
            Order originalOrder,
            Order replacementOrder,
            String ownerId,
            long ownerEpoch
    ) {
        String symbol = normalize(symbolCode);
        long offset = offsets.computeIfAbsent(symbol, ignored -> new AtomicLong()).incrementAndGet();
        MatchingCommandLogEntry entry = new MatchingCommandLogEntry(
                symbol,
                offset,
                MatchingCommandType.CANCEL_REPLACE,
                originalOrder,
                replacementOrder,
                null,
                null,
                ownerId,
                Math.max(0L, ownerEpoch),
                Instant.now()
        );
        entries.computeIfAbsent(symbol, ignored -> new ArrayList<>()).add(entry);
        return entry;
    }

    @Override
    public List<MatchingCommandLogEntry> listAfter(String symbolCode, long offset) {
        return listAll(symbolCode).stream()
                .filter(entry -> entry.offset() > offset)
                .sorted(Comparator.comparingLong(MatchingCommandLogEntry::offset))
                .toList();
    }

    @Override
    public List<MatchingCommandLogEntry> listAll(String symbolCode) {
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
