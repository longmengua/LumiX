/*
 * 檔案用途：應用服務測試，驗證 matching worker startup 的 lease acquire、recovery 與 readiness context。
 */
package com.example.exchange.application.service;

import com.example.exchange.domain.model.dto.MatchingCommandLogEntry;
import com.example.exchange.domain.model.dto.MatchingEngineSnapshot;
import com.example.exchange.domain.model.dto.MatchingRecoveryResult;
import com.example.exchange.domain.model.dto.MatchingReplayValidationReport;
import com.example.exchange.domain.model.dto.MatchingSequencerLease;
import com.example.exchange.domain.model.entity.Order;
import com.example.exchange.domain.model.entity.Symbol;
import com.example.exchange.domain.model.enums.MatchingCommandType;
import com.example.exchange.domain.model.enums.OrderSide;
import com.example.exchange.domain.model.enums.OrderType;
import com.example.exchange.domain.repository.MatchingCommandLog;
import com.example.exchange.domain.repository.MatchingReplayValidationReportStore;
import com.example.exchange.domain.repository.MatchingSequencerLeaseStore;
import com.example.exchange.domain.repository.MatchingSnapshotStore;
import com.example.exchange.infra.config.MatchingWorkerProperties;
import com.example.exchange.infra.matching.InMemoryMatchingCommandLog;
import com.example.exchange.infra.matching.InMemoryMatchingEngine;
import com.example.exchange.infra.matching.InMemoryMatchingEventLog;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MatchingWorkerLifecycleServiceTest {

    @Test
    @DisplayName("enabled worker startup 會 acquire lease、執行 recovery 並保存 owner context")
    /**
     * 流程：設定 worker owner/symbols -> startConfiguredSymbols ->
     * 驗證 lease、recovery result、owner context 都可供後續 command intake 使用。
     */
    void enabledWorkerStartupAcquiresLeaseRunsRecoveryAndStoresOwnerContext() {
        Fixture fixture = new Fixture(true, List.of("btcusdt"));
        fixture.commandLog.append("BTCUSDT", MatchingCommandType.SUBMIT, limitOrder(), null, null);

        List<MatchingWorkerLifecycleService.MatchingWorkerStartupResult> results =
                fixture.lifecycle.startConfiguredSymbols();

        assertThat(results).hasSize(1);
        MatchingWorkerLifecycleService.MatchingWorkerOwnerContext context =
                fixture.lifecycle.ownerContext("BTCUSDT").orElseThrow();
        assertThat(context.ownerId()).isEqualTo("worker-a");
        assertThat(context.ownerEpoch()).isEqualTo(1L);
        assertThat(context.recoveredCommandOffset()).isEqualTo(1L);
        assertThat(context.recoveredEventOffset()).isEqualTo(0L);
        assertThat(results.getFirst().recoveryResult())
                .extracting(MatchingRecoveryResult::validationValid)
                .isEqualTo(true);
    }

    /**
     * Recovery replay 需要完整 order snapshot；至少要有 symbol、side、type、price、qty。
     */
    private static Order limitOrder() {
        return Order.builder()
                .uid(1L)
                .symbol(Symbol.builder().base("BTC").quote("USDT").priceScale(2).qtyScale(3).build())
                .side(OrderSide.BUY)
                .type(OrderType.LIMIT)
                .price(new java.math.BigDecimal("100"))
                .qty(new java.math.BigDecimal("1"))
                .origQty(new java.math.BigDecimal("1"))
                .build();
    }

    @Test
    @DisplayName("disabled worker startup 不會 acquire lease 或 recovery")
    /**
     * 流程：matching-worker.enabled=false -> startConfiguredSymbols -> 驗證不做任何 symbol startup。
     */
    void disabledWorkerStartupDoesNothing() {
        Fixture fixture = new Fixture(false, List.of("BTCUSDT"));

        assertThat(fixture.lifecycle.startConfiguredSymbols()).isEmpty();
        assertThat(fixture.lifecycle.ownerContexts()).isEmpty();
        assertThat(fixture.leaseStore.current("BTCUSDT")).isEmpty();
    }

    @Test
    @DisplayName("lease 被其他 owner 持有時 startup 會拒絕 readiness")
    /**
     * 流程：other-owner 先持有 lease -> worker-a 嘗試 start 同 symbol -> acquire 失敗且不保存 owner context。
     */
    void startupFailsWhenLeaseIsOwnedByAnotherWorker() {
        Fixture fixture = new Fixture(true, List.of("BTCUSDT"));
        fixture.leaseStore.acquire("BTCUSDT", "other-worker", Duration.ofSeconds(30), fixture.clock.instant());

        assertThatThrownBy(() -> fixture.lifecycle.startConfiguredSymbols())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("lease acquire failed");
        assertThat(fixture.lifecycle.ownerContext("BTCUSDT")).isEmpty();
    }

    @Test
    @DisplayName("worker renewal 會更新 lease checkpoint 與 readiness context")
    /**
     * 流程：startup 後推進時間與 command log offset -> renewOwnedSymbols ->
     * 驗證 lease expiry 延長，且 owner context 反映最新 command/event checkpoint。
     */
    void renewalUpdatesLeaseCheckpointAndReadinessContext() {
        Fixture fixture = new Fixture(true, List.of("BTCUSDT"));
        fixture.commandLog.append("BTCUSDT", MatchingCommandType.SUBMIT, limitOrder(), null, null);
        fixture.lifecycle.startConfiguredSymbols();
        fixture.commandLog.append("BTCUSDT", MatchingCommandType.CANCEL, limitOrder(), null, null);
        fixture.clock.advance(Duration.ofSeconds(5));

        List<MatchingWorkerLifecycleService.MatchingWorkerOwnerContext> renewed =
                fixture.lifecycle.renewOwnedSymbols();

        assertThat(renewed).hasSize(1);
        MatchingWorkerLifecycleService.MatchingWorkerOwnerContext context = renewed.getFirst();
        assertThat(context.leaseExpiresAt()).isEqualTo(Instant.parse("2026-05-29T00:00:35Z"));
        assertThat(context.recoveredCommandOffset()).isEqualTo(2L);
        assertThat(fixture.leaseStore.current("BTCUSDT")).get()
                .extracting(MatchingSequencerLease::commandOffset, MatchingSequencerLease::eventOffset)
                .containsExactly(2L, 0L);
    }

    @Test
    @DisplayName("worker renewal 被拒絕時會移除 readiness")
    /**
     * 流程：startup 後 lease 被外部 owner takeover -> renewSymbol ->
     * 驗證本 worker readiness 被清掉，後續 command intake 無 owner context 可用。
     */
    void renewalFailureRemovesReadinessContext() {
        Fixture fixture = new Fixture(true, List.of("BTCUSDT"));
        fixture.commandLog.append("BTCUSDT", MatchingCommandType.SUBMIT, limitOrder(), null, null);
        fixture.lifecycle.startConfiguredSymbols();
        fixture.leaseStore.forcePut(new MatchingSequencerLease(
                "BTCUSDT",
                "other-worker",
                2L,
                fixture.clock.instant().plusSeconds(30),
                1L,
                0L,
                fixture.clock.instant()
        ));

        assertThat(fixture.lifecycle.renewSymbol("BTCUSDT")).isEmpty();
        assertThat(fixture.lifecycle.isReady("BTCUSDT")).isFalse();
    }

    @Test
    @DisplayName("cutover fence 開啟時未 ready 的 configured symbol 不能 fallback 到舊路徑")
    /**
     * 流程：worker enabled + fenceLegacyRouting=true 但未 start symbol ->
     * routingOwnerContext 必須拒絕，避免 production 切流時落回 in-process writer。
     */
    void routingOwnerContextRejectsConfiguredSymbolWhenFenceEnabledAndNotReady() {
        Fixture fixture = new Fixture(true, List.of("BTCUSDT"));
        fixture.properties.setFenceLegacyRouting(true);

        assertThatThrownBy(() -> fixture.lifecycle.routingOwnerContext("BTCUSDT"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("owner context is not ready");
        assertThat(fixture.lifecycle.routingOwnerContext("ETHUSDT")).isEmpty();
    }

    @Test
    @DisplayName("runtime startup listener 只在 matching worker enabled 時啟動 configured symbols")
    /**
     * 流程：listener 收到 ApplicationReadyEvent 等價呼叫 -> enabled=false 不啟動；enabled=true 啟動一次。
     */
    void startupListenerStartsConfiguredSymbolsOnlyWhenEnabled() {
        CountingLifecycle disabledLifecycle = new CountingLifecycle();
        MatchingWorkerProperties disabled = new MatchingWorkerProperties();
        new MatchingWorkerStartupListener(disabled, disabledLifecycle).startConfiguredSymbolsWhenEnabled();

        CountingLifecycle enabledLifecycle = new CountingLifecycle();
        MatchingWorkerProperties enabled = new MatchingWorkerProperties();
        enabled.setEnabled(true);
        new MatchingWorkerStartupListener(enabled, enabledLifecycle).startConfiguredSymbolsWhenEnabled();

        assertThat(disabledLifecycle.starts).isZero();
        assertThat(enabledLifecycle.starts).isEqualTo(1);
    }

    private static final class Fixture {
        private final MutableClock clock = new MutableClock(Instant.parse("2026-05-29T00:00:00Z"));
        private final InMemoryLeaseStore leaseStore = new InMemoryLeaseStore();
        private final InMemoryMatchingCommandLog commandLog = new InMemoryMatchingCommandLog();
        private final InMemoryMatchingEventLog eventLog = new InMemoryMatchingEventLog();
        private final InMemoryMatchingSnapshotStore snapshotStore = new InMemoryMatchingSnapshotStore();
        private final InMemoryMatchingReplayReportStore reportStore = new InMemoryMatchingReplayReportStore();
        private final MatchingWorkerProperties properties = new MatchingWorkerProperties();
        private final MatchingWorkerLifecycleService lifecycle;

        private Fixture(boolean enabled, List<String> symbols) {
            properties.setEnabled(enabled);
            properties.setOwnerId("worker-a");
            properties.setSymbols(new ArrayList<>(symbols));
            properties.setLeaseTtlMs(30_000L);
            MatchingSequencerLeaseService leaseService = new MatchingSequencerLeaseService(leaseStore, clock);
            MatchingRecoveryService recoveryService = new MatchingRecoveryService(
                    new InMemoryMatchingEngine(commandLog, eventLog),
                    commandLog,
                    snapshotStore,
                    reportStore
            );
            lifecycle = new MatchingWorkerLifecycleService(properties, leaseService, recoveryService, commandLog, eventLog);
        }
    }

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
            String symbol = normalize(symbolCode);
            MatchingSequencerLease current = leases.get(symbol);
            if (current == null || !current.ownerId().equals(ownerId) || current.epoch() != epoch) {
                return Optional.empty();
            }
            MatchingSequencerLease renewed = new MatchingSequencerLease(
                    symbol,
                    ownerId,
                    epoch,
                    now.plus(ttl),
                    Math.max(0L, commandOffset),
                    Math.max(0L, eventOffset),
                    now
            );
            leases.put(symbol, renewed);
            return Optional.of(renewed);
        }

        @Override
        public boolean release(String symbolCode, String ownerId, long epoch, Instant now) {
            throw new UnsupportedOperationException("release is not needed by lifecycle startup tests");
        }

        @Override
        public Optional<MatchingSequencerLease> current(String symbolCode) {
            return Optional.ofNullable(leases.get(normalize(symbolCode)));
        }

        private void forcePut(MatchingSequencerLease lease) {
            leases.put(normalize(lease.symbolCode()), lease);
        }
    }

    private static final class CountingLifecycle extends MatchingWorkerLifecycleService {
        private int starts;

        private CountingLifecycle() {
            super(null, null, null, null, null);
        }

        @Override
        public List<MatchingWorkerStartupResult> startConfiguredSymbols() {
            starts++;
            return List.of();
        }
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

        private void advance(Duration duration) {
            instant = instant.plus(duration);
        }
    }

    private static String normalize(String symbolCode) {
        return symbolCode == null ? "" : symbolCode.trim().toUpperCase();
    }
}
