/*
 * 檔案用途：JPA adapter，實作 durable matching event log。
 */
package com.example.exchange.domain.repository.jpa;

import com.example.exchange.domain.event.TradeExecuted;
import com.example.exchange.domain.model.dto.MatchingEventLogEntry;
import com.example.exchange.domain.model.entity.MatchingEventLogRecord;
import com.example.exchange.domain.model.entity.MatchingOffsetCheckpointRecord;
import com.example.exchange.domain.repository.MatchingEventLog;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Primary
@Repository
@RequiredArgsConstructor
public class JpaMatchingEventLog implements MatchingEventLog {

    private final MatchingEventLogRecordJpaRepository repository;
    private final MatchingOffsetCheckpointRecordJpaRepository checkpointRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public MatchingEventLogEntry append(String symbolCode, long commandOffset, TradeExecuted trade) {
        String symbol = normalize(symbolCode);
        MatchingEventLogRecord record = new MatchingEventLogRecord();
        record.setSymbolCode(symbol);
        record.setOffsetValue(nextEventOffset(symbol));
        record.setCommandOffset(commandOffset);
        record.setTradePayload(writeTrade(trade));
        record.setCreatedAt(Instant.now());
        return toEntry(repository.save(record));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MatchingEventLogEntry> listAfter(String symbolCode, long offset) {
        return repository.findBySymbolCodeAndOffsetValueGreaterThanOrderByOffsetValueAsc(normalize(symbolCode), offset)
                .stream()
                .map(this::toEntry)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MatchingEventLogEntry> listAll(String symbolCode) {
        return repository.findBySymbolCodeOrderByOffsetValueAsc(normalize(symbolCode))
                .stream()
                .map(this::toEntry)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long lastOffset(String symbolCode) {
        return repository.lastOffset(normalize(symbolCode));
    }

    private MatchingEventLogEntry toEntry(MatchingEventLogRecord record) {
        return new MatchingEventLogEntry(
                record.getSymbolCode(),
                record.getOffsetValue(),
                record.getCommandOffset(),
                readTrade(record.getTradePayload()),
                record.getCreatedAt()
        );
    }

    private String writeTrade(TradeExecuted trade) {
        try {
            return objectMapper.writeValueAsString(trade);
        } catch (Exception e) {
            throw new IllegalStateException("serialize matching event trade failed", e);
        }
    }

    private TradeExecuted readTrade(String json) {
        try {
            return objectMapper.readValue(json, TradeExecuted.class);
        } catch (Exception e) {
            throw new IllegalStateException("deserialize matching event trade failed", e);
        }
    }

    private long nextEventOffset(String symbolCode) {
        MatchingOffsetCheckpointRecord checkpoint = checkpointRepository.findLocked(symbolCode)
                .orElseGet(() -> newCheckpoint(symbolCode));
        long nextOffset = checkpoint.getEventOffset() + 1;
        checkpoint.setEventOffset(nextOffset);
        checkpoint.setUpdatedAt(Instant.now());
        checkpointRepository.save(checkpoint);
        return nextOffset;
    }

    private MatchingOffsetCheckpointRecord newCheckpoint(String symbolCode) {
        MatchingOffsetCheckpointRecord checkpoint = new MatchingOffsetCheckpointRecord();
        checkpoint.setSymbolCode(symbolCode);
        checkpoint.setCommandOffset(0L);
        // 若 migration 前已有 log，可用 lastOffset 對齊初始 checkpoint；新庫則從 0 開始。
        checkpoint.setEventOffset(repository.lastOffset(symbolCode));
        checkpoint.setUpdatedAt(Instant.now());
        return checkpoint;
    }

    private static String normalize(String symbolCode) {
        return symbolCode == null ? "" : symbolCode.trim().toUpperCase();
    }
}
