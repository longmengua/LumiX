/*
 * 檔案用途：應用服務測試，驗證 production matching worker command routing 的 lease fencing。
 */
package com.example.exchange.application.service;

import com.example.exchange.domain.event.TradeExecuted;
import com.example.exchange.domain.model.dto.MatchingCommandLogEntry;
import com.example.exchange.domain.model.dto.MatchingEventLogEntry;
import com.example.exchange.domain.model.dto.MatchingSequencerLease;
import com.example.exchange.domain.model.entity.Order;
import com.example.exchange.domain.model.entity.Symbol;
import com.example.exchange.domain.model.enums.MatchingCommandType;
import com.example.exchange.domain.model.enums.OrderSide;
import com.example.exchange.domain.model.enums.OrderType;
import com.example.exchange.domain.repository.MatchingSequencerLeaseStore;
import com.example.exchange.infra.matching.InMemoryMatchingCommandLog;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MatchingWorkerCommandRouterTest {

    private final Symbol symbol = Symbol.builder()
            .base("BTC")
            .quote("USDT")
            .priceScale(2)
            .qtyScale(3)
            .build();

    @Test
    @DisplayName("valid owner 可以 append command 且 owner epoch 會寫入 audit 欄位")
    /**
     * 流程：worker acquire symbol lease -> router append command -> 驗證 command log 保存 owner/epoch。
     */
    void validOwnerCanAppendCommandWithOwnerEpoch() {
        Fixture fixture = new Fixture();
        MatchingSequencerLease lease = fixture.leaseService.acquire("BTCUSDT", "worker-a", Duration.ofSeconds(10))
                .orElseThrow();

        MatchingCommandLogEntry entry = fixture.router.appendCommand(
                "BTCUSDT",
                MatchingCommandType.SUBMIT,
                limit(1, OrderSide.BUY, "100", "1"),
                null,
                null,
                "worker-a",
                lease.epoch()
        );

        assertThat(entry.ownerId()).isEqualTo("worker-a");
        assertThat(entry.ownerEpoch()).isEqualTo(lease.epoch());
        assertThat(fixture.commandLog.listAll("BTCUSDT")).hasSize(1);
    }

    @Test
    @DisplayName("wrong owner、stale epoch、expired lease 都不能 append command")
    /**
     * 流程：worker-a acquire -> wrong owner 被拒 -> stale epoch 被拒 -> lease 過期後原 owner 也被拒。
     */
    void rejectsWrongOwnerStaleEpochAndExpiredLeaseBeforeCommandAppend() {
        Fixture fixture = new Fixture();
        MatchingSequencerLease lease = fixture.leaseService.acquire("BTCUSDT", "worker-a", Duration.ofSeconds(2))
                .orElseThrow();
        Order order = limit(1, OrderSide.BUY, "100", "1");

        assertThatThrownBy(() -> fixture.router.appendCommand(
                "BTCUSDT", MatchingCommandType.SUBMIT, order, null, null, "worker-b", lease.epoch()
        )).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("owner mismatch");

        assertThatThrownBy(() -> fixture.router.appendCommand(
                "BTCUSDT", MatchingCommandType.SUBMIT, order, null, null, "worker-a", lease.epoch() - 1
        )).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("stale epoch");

        fixture.clock.advance(Duration.ofSeconds(3));
        assertThatThrownBy(() -> fixture.router.appendCommand(
                "BTCUSDT", MatchingCommandType.SUBMIT, order, null, null, "worker-a", lease.epoch()
        )).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("lease expired");
        assertThat(fixture.commandLog.listAll("BTCUSDT")).isEmpty();
    }

    @Test
    @DisplayName("event append 也必須通過同一個 owner epoch guard")
    /**
     * 流程：worker acquire -> router append matching event -> 驗證 event log 保存 owner/epoch。
     */
    void validOwnerCanAppendEventWithOwnerEpoch() {
        Fixture fixture = new Fixture();
        MatchingSequencerLease lease = fixture.leaseService.acquire("BTCUSDT", "worker-a", Duration.ofSeconds(10))
                .orElseThrow();
        TradeExecuted trade = new TradeExecuted(
                1L,
                symbol,
                new BigDecimal("1"),
                new BigDecimal("100"),
                0L,
                Instant.now()
        );

        MatchingEventLogEntry entry = fixture.router.appendEvent("BTCUSDT", 1L, trade, "worker-a", lease.epoch());

        assertThat(entry.ownerId()).isEqualTo("worker-a");
        assertThat(entry.ownerEpoch()).isEqualTo(lease.epoch());
        assertThat(fixture.eventLog.listAll("BTCUSDT")).hasSize(1);
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
        private final MatchingWorkerCommandRouter router =
                new MatchingWorkerCommandRouter(leaseService, commandLog, eventLog);
    }

    /**
     * 測試專用 lease store；固定 owner/epoch/expiry 語義，讓 router 測試只關注 append guard。
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
            throw new UnsupportedOperationException("renew is not needed by router append tests");
        }

        @Override
        public boolean release(String symbolCode, String ownerId, long epoch, Instant now) {
            throw new UnsupportedOperationException("release is not needed by router append tests");
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

        private void advance(Duration duration) {
            instant = instant.plus(duration);
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
