package com.lumix.trading.core.spot.orderbook;

import com.lumix.trading.core.spot.orderintake.SpotSandboxOrderCommand;
import com.lumix.trading.core.spot.orderintake.SpotSandboxOrderIntakeResult;
import com.lumix.trading.core.spot.orderintake.SpotSandboxOrderDecision;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory spot sandbox order book。
 *
 * 這個 book 只存在於記憶體，不代表已持久化、已 matching、已 settlement 或已接上正式交易 runtime。
 */
public final class InMemorySpotSandboxOrderBook {

    private final AtomicLong sandboxOrderSequence = new AtomicLong();
    private final Map<String, SpotSandboxOrderRecord> recordsByIdempotencyKey = new LinkedHashMap<>();
    private final Map<String, List<SpotSandboxOrderRecord>> recordsByMarket = new LinkedHashMap<>();
    private final Map<String, List<SpotSandboxOrderRecord>> openRecordsByMarket = new LinkedHashMap<>();

    /**
     * 把已通過 intake 的 order result 放入 in-memory book。
     *
     * 這裡只做 sandbox record materialization，不代表任何 DB write 或後續交易流程已完成。
     */
    public synchronized SpotSandboxOrderBookResult accept(SpotSandboxOrderIntakeResult intakeResult, Instant acceptedAt) {
        Objects.requireNonNull(intakeResult, "intakeResult must not be null");
        Objects.requireNonNull(acceptedAt, "acceptedAt must not be null");

        if (intakeResult.decision() != SpotSandboxOrderDecision.ACCEPTED || intakeResult.command() == null) {
            return SpotSandboxOrderBookResult.rejected(
                    SpotSandboxOrderBookRejectionReason.INTAKE_REJECTED,
                    "intake result 必須先通過 P16-T02 accepted validation"
            );
        }

        return accept(intakeResult.command(), acceptedAt);
    }

    /**
     * 把 accepted command materialize 成 in-memory sandbox record。
     *
     * 這裡不會建立第二筆不同 sandboxOrderId 來對應同一個 idempotencyKey。
     */
    public synchronized SpotSandboxOrderBookResult accept(SpotSandboxOrderCommand command, Instant acceptedAt) {
        Objects.requireNonNull(command, "command must not be null");
        Objects.requireNonNull(acceptedAt, "acceptedAt must not be null");

        SpotSandboxOrderRecord existing = recordsByIdempotencyKey.get(command.idempotencyKey());
        if (existing != null) {
            return SpotSandboxOrderBookResult.duplicate(existing);
        }

        SpotSandboxOrderRecord record = new SpotSandboxOrderRecord(
                nextSandboxOrderId(),
                command.requestId(),
                command.idempotencyKey(),
                command.userId(),
                command.accountId(),
                command.marketSymbol(),
                command.side(),
                command.type(),
                command.price(),
                command.quantity(),
                command.quantity(),
                command.timeInForce(),
                SpotSandboxOrderStatus.OPEN,
                acceptedAt
        );

        recordsByIdempotencyKey.put(record.idempotencyKey(), record);
        recordsByMarket.computeIfAbsent(record.marketSymbol(), key -> new ArrayList<>()).add(record);
        openRecordsByMarket.computeIfAbsent(record.marketSymbol(), key -> new ArrayList<>()).add(record);
        return SpotSandboxOrderBookResult.accepted(record);
    }

    /**
     * 查詢指定 market 的 open orders。
     *
     * 這個查詢只回傳 in-memory book 內的 open records，不代表任何 production order book。
     */
    public synchronized List<SpotSandboxOrderRecord> openOrders(String marketSymbol) {
        Objects.requireNonNull(marketSymbol, "marketSymbol must not be null");
        return openRecordsByMarket.getOrDefault(marketSymbol, List.of()).stream()
                .filter(InMemorySpotSandboxOrderBook::isActiveOrder)
                .toList();
    }

    /**
     * 查詢指定 market 是否曾經存在於 in-memory book。
     *
     * 這個查詢只服務 sandbox matching 的 market boundary，不代表任何 production order book。
     */
    public synchronized boolean hasMarketSymbol(String marketSymbol) {
        Objects.requireNonNull(marketSymbol, "marketSymbol must not be null");
        return recordsByMarket.containsKey(marketSymbol);
    }

    /**
     * 套用 matching 後的 in-memory record 更新。
     *
     * 這個方法只允許 sandbox matching runtime 調整 remainingQuantity 與 status，不代表任何 DB write。
     */
    public synchronized void replaceRecord(SpotSandboxOrderRecord updatedRecord) {
        Objects.requireNonNull(updatedRecord, "updatedRecord must not be null");

        SpotSandboxOrderRecord previous = recordsByIdempotencyKey.put(updatedRecord.idempotencyKey(), updatedRecord);
        if (previous == null) {
            recordsByMarket.computeIfAbsent(updatedRecord.marketSymbol(), key -> new ArrayList<>()).add(updatedRecord);
        } else {
            replaceRecord(recordsByMarket, previous.marketSymbol(), previous.idempotencyKey(), updatedRecord);
        }

        if (isActiveOrder(updatedRecord)) {
            replaceRecord(openRecordsByMarket, updatedRecord.marketSymbol(), updatedRecord.idempotencyKey(), updatedRecord);
        } else {
            removeRecord(openRecordsByMarket, previous != null ? previous.marketSymbol() : updatedRecord.marketSymbol(), updatedRecord.idempotencyKey());
        }
    }

    /**
     * 依 idempotencyKey 查詢既有 record。
     *
     * 這個查詢只服務 sandbox duplicate protection，不代表正式 idempotency store。
     */
    public synchronized Optional<SpotSandboxOrderRecord> findByIdempotencyKey(String idempotencyKey) {
        Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null");
        return Optional.ofNullable(recordsByIdempotencyKey.get(idempotencyKey));
    }

    /**
     * 回傳目前 in-memory book 內的 record 數量。
     *
     * 這個方法只用於測試與觀測，不代表任何持久化狀態。
     */
    public synchronized int size() {
        return recordsByIdempotencyKey.size();
    }

    private String nextSandboxOrderId() {
        return "sandbox-order-" + sandboxOrderSequence.incrementAndGet();
    }

    private static void replaceRecord(
            Map<String, List<SpotSandboxOrderRecord>> recordsByMarket,
            String marketSymbol,
            String idempotencyKey,
            SpotSandboxOrderRecord updatedRecord
    ) {
        List<SpotSandboxOrderRecord> records = recordsByMarket.get(marketSymbol);
        if (records == null) {
            recordsByMarket.computeIfAbsent(marketSymbol, key -> new ArrayList<>()).add(updatedRecord);
            return;
        }

        for (int index = 0; index < records.size(); index++) {
            if (records.get(index).idempotencyKey().equals(idempotencyKey)) {
                records.set(index, updatedRecord);
                return;
            }
        }

        records.add(updatedRecord);
    }

    private static void removeRecord(
            Map<String, List<SpotSandboxOrderRecord>> recordsByMarket,
            String marketSymbol,
            String idempotencyKey
    ) {
        List<SpotSandboxOrderRecord> records = recordsByMarket.get(marketSymbol);
        if (records == null) {
            return;
        }

        records.removeIf(record -> record.idempotencyKey().equals(idempotencyKey));
        if (records.isEmpty()) {
            recordsByMarket.remove(marketSymbol);
        }
    }

    /**
     * 確認一筆 sandbox order 是否仍屬 active。
     *
     * 這裡只服務 in-memory order book 與 matching runtime，不代表正式 order state machine 已完成。
     */
    public static boolean isActiveOrder(SpotSandboxOrderRecord record) {
        Objects.requireNonNull(record, "record must not be null");
        if (record.remainingQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }

        return record.status() == SpotSandboxOrderStatus.OPEN
                || record.status() == SpotSandboxOrderStatus.PARTIALLY_FILLED;
    }
}
