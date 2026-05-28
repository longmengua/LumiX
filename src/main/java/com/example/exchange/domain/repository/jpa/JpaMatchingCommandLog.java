/*
 * 檔案用途：JPA adapter，實作 durable matching command log。
 */
package com.example.exchange.domain.repository.jpa;

import com.example.exchange.domain.model.dto.MatchingCommandLogEntry;
import com.example.exchange.domain.model.entity.MatchingCommandLogRecord;
import com.example.exchange.domain.model.entity.MatchingOffsetCheckpointRecord;
import com.example.exchange.domain.model.entity.Order;
import com.example.exchange.domain.model.enums.MatchingCommandType;
import com.example.exchange.domain.repository.MatchingCommandLog;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Primary
@Repository
@RequiredArgsConstructor
public class JpaMatchingCommandLog implements MatchingCommandLog {

    private final MatchingCommandLogRecordJpaRepository repository;
    private final MatchingOffsetCheckpointRecordJpaRepository checkpointRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
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
    @Transactional
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
        MatchingCommandLogRecord record = new MatchingCommandLogRecord();
        record.setSymbolCode(symbol);
        record.setOffsetValue(nextCommandOffset(symbol));
        record.setCommandType(type.name());
        record.setOrderPayload(writeOrder(order));
        record.setReplacementOrderPayload(null);
        record.setNewPrice(newPrice);
        record.setNewQty(newQty);
        record.setOwnerId(ownerId);
        record.setOwnerEpoch(Math.max(0L, ownerEpoch));
        record.setCreatedAt(Instant.now());
        return toEntry(repository.save(record));
    }

    @Override
    @Transactional
    public MatchingCommandLogEntry appendCancelReplace(String symbolCode, Order originalOrder, Order replacementOrder) {
        return appendCancelReplace(symbolCode, originalOrder, replacementOrder, null, 0L);
    }

    @Override
    @Transactional
    public MatchingCommandLogEntry appendCancelReplace(
            String symbolCode,
            Order originalOrder,
            Order replacementOrder,
            String ownerId,
            long ownerEpoch
    ) {
        String symbol = normalize(symbolCode);
        MatchingCommandLogRecord record = new MatchingCommandLogRecord();
        record.setSymbolCode(symbol);
        record.setOffsetValue(nextCommandOffset(symbol));
        record.setCommandType(MatchingCommandType.CANCEL_REPLACE.name());
        record.setOrderPayload(writeOrder(originalOrder));
        record.setReplacementOrderPayload(writeOrder(replacementOrder));
        record.setNewPrice(null);
        record.setNewQty(null);
        record.setOwnerId(ownerId);
        record.setOwnerEpoch(Math.max(0L, ownerEpoch));
        record.setCreatedAt(Instant.now());
        return toEntry(repository.save(record));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MatchingCommandLogEntry> listAfter(String symbolCode, long offset) {
        return repository.findBySymbolCodeAndOffsetValueGreaterThanOrderByOffsetValueAsc(normalize(symbolCode), offset)
                .stream()
                .map(this::toEntry)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MatchingCommandLogEntry> listAll(String symbolCode) {
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

    private MatchingCommandLogEntry toEntry(MatchingCommandLogRecord record) {
        return new MatchingCommandLogEntry(
                record.getSymbolCode(),
                record.getOffsetValue(),
                MatchingCommandType.valueOf(record.getCommandType()),
                readOrder(record.getOrderPayload()),
                readNullableOrder(record.getReplacementOrderPayload()),
                record.getNewPrice(),
                record.getNewQty(),
                record.getOwnerId(),
                record.getOwnerEpoch() == null ? 0L : record.getOwnerEpoch(),
                record.getCreatedAt()
        );
    }

    private String writeOrder(Order order) {
        try {
            return objectMapper.writeValueAsString(order);
        } catch (Exception e) {
            throw new IllegalStateException("serialize matching command order failed", e);
        }
    }

    private Order readOrder(String json) {
        try {
            return objectMapper.readValue(json, Order.class);
        } catch (Exception e) {
            throw new IllegalStateException("deserialize matching command order failed", e);
        }
    }

    private Order readNullableOrder(String json) {
        if (json == null || json.isBlank()) return null;
        return readOrder(json);
    }

    private long nextCommandOffset(String symbolCode) {
        MatchingOffsetCheckpointRecord checkpoint = checkpointRepository.findLocked(symbolCode)
                .orElseGet(() -> newCheckpoint(symbolCode));
        long nextOffset = checkpoint.getCommandOffset() + 1;
        checkpoint.setCommandOffset(nextOffset);
        checkpoint.setUpdatedAt(Instant.now());
        checkpointRepository.save(checkpoint);
        return nextOffset;
    }

    private MatchingOffsetCheckpointRecord newCheckpoint(String symbolCode) {
        MatchingOffsetCheckpointRecord checkpoint = new MatchingOffsetCheckpointRecord();
        checkpoint.setSymbolCode(symbolCode);
        // 若 migration 前已有 log，可用 lastOffset 對齊初始 checkpoint；新庫則從 0 開始。
        checkpoint.setCommandOffset(repository.lastOffset(symbolCode));
        checkpoint.setEventOffset(0L);
        checkpoint.setUpdatedAt(Instant.now());
        return checkpoint;
    }

    private static String normalize(String symbolCode) {
        return symbolCode == null ? "" : symbolCode.trim().toUpperCase();
    }
}
