/*
 * 檔案用途：應用服務測試，驗證 production matching worker 從 fenced command append 到 engine execution 的流程。
 */
package com.example.exchange.application.service;

import com.example.exchange.domain.model.dto.MatchingEventLogEntry;
import com.example.exchange.domain.model.dto.MatchingResult;
import com.example.exchange.domain.model.dto.MatchingSequencerLease;
import com.example.exchange.domain.model.entity.Order;
import com.example.exchange.domain.model.entity.Symbol;
import com.example.exchange.domain.model.enums.MatchingCommandType;
import com.example.exchange.domain.model.enums.OrderSide;
import com.example.exchange.domain.model.enums.OrderType;
import com.example.exchange.domain.repository.MatchingSequencerLeaseStore;
import com.example.exchange.infra.matching.InMemoryMatchingCommandLog;
import com.example.exchange.infra.matching.InMemoryMatchingEngine;
import com.example.exchange.infra.matching.InMemoryMatchingEventLog;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;

class MatchingWorkerExecutionServiceTest {

    private final Symbol symbol = Symbol.builder()
            .base("BTC")
            .quote("USDT")
            .priceScale(2)
            .qtyScale(3)
            .build();

    @Test
    @DisplayName("worker submit 會先 append fenced command 再執行 engine，且 event 保留 owner epoch")
    /**
     * 流程：先放入 resting buy -> worker 持有 lease 後送 sell ->
     * 驗證 command log 不重複 append，trade event 繼承 worker owner/epoch。
     */
    void workerSubmitAppendsFencedCommandThenExecutesEngine() {
        Fixture fixture = new Fixture();
        fixture.engine.submit(limit(1, OrderSide.BUY, "100", "1"));
        MatchingSequencerLease lease = fixture.leaseService.acquire("BTCUSDT", "worker-a", Duration.ofSeconds(10))
                .orElseThrow();

        MatchingResult result = fixture.workerExecution.submit(
                limit(2, OrderSide.SELL, "100", "1"),
                "worker-a",
                lease.epoch()
        );

        assertThat(result.getTrades()).isNotEmpty();
        assertThat(fixture.commandLog.listAll("BTCUSDT")).hasSize(2);
        assertThat(fixture.commandLog.listAll("BTCUSDT").getLast().ownerId()).isEqualTo("worker-a");
        assertThat(fixture.eventLog.listAll("BTCUSDT"))
                .extracting(MatchingEventLogEntry::ownerEpoch)
                .containsOnly(lease.epoch());
    }

    @Test
    @DisplayName("worker cancel 會 append fenced cancel command 並從 book 移除掛單")
    /**
     * 流程：worker submit resting order -> worker cancel 同一張單 ->
     * 驗證 cancel command 有 owner/epoch，且 order book 不再有 best bid。
     */
    void workerCancelAppendsFencedCommandAndRemovesOrder() {
        Fixture fixture = new Fixture();
        MatchingSequencerLease lease = fixture.leaseService.acquire("BTCUSDT", "worker-a", Duration.ofSeconds(10))
                .orElseThrow();
        Order order = limit(1, OrderSide.BUY, "100", "1");
        fixture.workerExecution.submit(order, "worker-a", lease.epoch());

        fixture.workerExecution.cancel(order, "worker-a", lease.epoch());

        assertThat(fixture.commandLog.listAll("BTCUSDT").getLast().type()).isEqualTo(MatchingCommandType.CANCEL);
        assertThat(fixture.commandLog.listAll("BTCUSDT").getLast().ownerEpoch()).isEqualTo(lease.epoch());
        assertThat(fixture.engine.top("BTCUSDT")).isEmpty();
    }

    @Test
    @DisplayName("worker amend 會 append fenced amend command 並更新 book price")
    /**
     * 流程：worker submit resting order -> worker amend price ->
     * 驗證 amend command 有 owner/epoch，且 top of book 反映新價格。
     */
    void workerAmendAppendsFencedCommandAndUpdatesBook() {
        Fixture fixture = new Fixture();
        MatchingSequencerLease lease = fixture.leaseService.acquire("BTCUSDT", "worker-a", Duration.ofSeconds(10))
                .orElseThrow();
        Order order = limit(1, OrderSide.BUY, "100", "1");
        fixture.workerExecution.submit(order, "worker-a", lease.epoch());

        fixture.workerExecution.amend(order, new BigDecimal("101"), new BigDecimal("1"), "worker-a", lease.epoch());

        assertThat(fixture.commandLog.listAll("BTCUSDT").getLast().type()).isEqualTo(MatchingCommandType.AMEND);
        assertThat(fixture.commandLog.listAll("BTCUSDT").getLast().ownerId()).isEqualTo("worker-a");
        assertThat(fixture.engine.top("BTCUSDT")).get()
                .satisfies(top -> assertThat(top.getBestBid()).isEqualByComparingTo("101"));
    }

    @Test
    @DisplayName("worker cancel-replace 會 append 單一 fenced command 並送 replacement 進 book")
    /**
     * 流程：worker submit original -> worker cancel-replace ->
     * 驗證只新增一筆 CANCEL_REPLACE command，且 replacement 成為新的 best bid。
     */
    void workerCancelReplaceAppendsSingleFencedCommandAndPlacesReplacement() {
        Fixture fixture = new Fixture();
        MatchingSequencerLease lease = fixture.leaseService.acquire("BTCUSDT", "worker-a", Duration.ofSeconds(10))
                .orElseThrow();
        Order original = limit(1, OrderSide.BUY, "100", "1");
        Order replacement = limit(1, OrderSide.BUY, "101", "1");
        fixture.workerExecution.submit(original, "worker-a", lease.epoch());

        fixture.workerExecution.cancelReplace(original, replacement, "worker-a", lease.epoch());

        assertThat(fixture.commandLog.listAll("BTCUSDT")).hasSize(2);
        assertThat(fixture.commandLog.listAll("BTCUSDT").getLast().type()).isEqualTo(MatchingCommandType.CANCEL_REPLACE);
        assertThat(fixture.commandLog.listAll("BTCUSDT").getLast().ownerEpoch()).isEqualTo(lease.epoch());
        assertThat(fixture.engine.top("BTCUSDT")).get()
                .satisfies(top -> assertThat(top.getBestBid()).isEqualByComparingTo("101"));
    }

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

    private static final class Fixture {
        private final MutableClock clock = new MutableClock(Instant.parse("2026-05-29T00:00:00Z"));
        private final MatchingSequencerLeaseService leaseService =
                new MatchingSequencerLeaseService(new InMemoryLeaseStore(), clock);
        private final InMemoryMatchingCommandLog commandLog = new InMemoryMatchingCommandLog();
        private final InMemoryMatchingEventLog eventLog = new InMemoryMatchingEventLog();
        private final InMemoryMatchingEngine engine = new InMemoryMatchingEngine(commandLog, eventLog);
        private final MatchingWorkerCommandRouter router =
                new MatchingWorkerCommandRouter(leaseService, commandLog, eventLog);
        private final MatchingWorkerExecutionService workerExecution =
                new MatchingWorkerExecutionService(router, engine);
    }

    /**
     * 測試專用 lease store；只實作 acquire/current，足以驗證 worker submit fencing。
     */
    private static final class InMemoryLeaseStore implements MatchingSequencerLeaseStore {
        private final Map<String, MatchingSequencerLease> leases = new ConcurrentHashMap<>();

        @Override
        public Optional<MatchingSequencerLease> acquire(String symbolCode, String ownerId, Duration ttl, Instant now) {
            String symbol = normalize(symbolCode);
            MatchingSequencerLease current = leases.get(symbol);
            if (current != null && !current.ownerId().equals(ownerId) && current.expiresAt().isAfter(now)) {
                return Optional.empty();
            }
            long nextEpoch = current == null
                    ? 1L
                    : current.ownerId().equals(ownerId) ? current.epoch() : current.epoch() + 1;
            MatchingSequencerLease acquired = new MatchingSequencerLease(
                    symbol,
                    ownerId,
                    nextEpoch,
                    now.plus(ttl),
                    current == null ? 0L : current.commandOffset(),
                    current == null ? 0L : current.eventOffset(),
                    now
            );
            leases.put(symbol, acquired);
            return Optional.of(acquired);
        }

        @Override
        public Optional<MatchingSequencerLease> renew(
                String symbolCode,
                String ownerId,
                long epoch,
                Duration ttl,
                long commandOffset,
                long eventOffset,
                Instant now
        ) {
            throw new UnsupportedOperationException("renew is not needed by worker execution tests");
        }

        @Override
        public boolean release(String symbolCode, String ownerId, long epoch, Instant now) {
            throw new UnsupportedOperationException("release is not needed by worker execution tests");
        }

        @Override
        public Optional<MatchingSequencerLease> current(String symbolCode) {
            return Optional.ofNullable(leases.get(normalize(symbolCode)));
        }
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }

    private static String normalize(String symbolCode) {
        return symbolCode == null ? "" : symbolCode.trim().toUpperCase();
    }
}
