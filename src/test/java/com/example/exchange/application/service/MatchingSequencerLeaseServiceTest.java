/*
 * 檔案用途：測試 matching sequencer lease service 的 owner / epoch lifecycle。
 */
package com.example.exchange.application.service;

import com.example.exchange.domain.model.dto.MatchingSequencerLease;
import com.example.exchange.domain.repository.MatchingSequencerLeaseStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * MatchingSequencerLeaseService tests。
 *
 * <p>以 in-memory store 固定 lease 語義：同一時間只有 lease owner 能續租，過期或釋放後
 * 新 owner 才能取得更高 epoch。</p>
 */
class MatchingSequencerLeaseServiceTest {

    @Test
    @DisplayName("active lease 會阻擋其他 owner，正確 owner 可續租並推進 checkpoint")
    /**
     * 流程：owner-a acquire -> owner-b 在未過期前 acquire 失敗 ->
     * owner-a renew 推進 offset -> wrong epoch release 失敗 -> 正確 release 後 owner-b 接手。
     */
    void activeLeaseBlocksOtherOwnersAndAllowsOwnerRenewal() {
        MutableClock clock = new MutableClock(Instant.parse("2026-05-27T00:00:00Z"));
        MatchingSequencerLeaseService service = new MatchingSequencerLeaseService(new InMemoryLeaseStore(), clock);

        MatchingSequencerLease ownerA = service.acquire("btcusdt", "owner-a", Duration.ofSeconds(10)).orElseThrow();
        Optional<MatchingSequencerLease> blockedOwnerB = service.acquire("BTCUSDT", "owner-b", Duration.ofSeconds(10));
        MatchingSequencerLease renewed = service.renew("BTCUSDT", "owner-a", ownerA.epoch(), 7L, 11L).orElseThrow();

        assertThat(ownerA.symbolCode()).isEqualTo("BTCUSDT");
        assertThat(ownerA.epoch()).isEqualTo(1L);
        assertThat(blockedOwnerB).isEmpty();
        assertThat(renewed.commandOffset()).isEqualTo(7L);
        assertThat(renewed.eventOffset()).isEqualTo(11L);
        assertThat(service.release("BTCUSDT", "owner-a", ownerA.epoch() + 1)).isFalse();
        assertThat(service.release("BTCUSDT", "owner-a", ownerA.epoch())).isTrue();
        assertThat(service.acquire("BTCUSDT", "owner-b", Duration.ofSeconds(10))).get()
                .extracting(MatchingSequencerLease::ownerId, MatchingSequencerLease::epoch)
                .containsExactly("owner-b", 2L);
    }

    @Test
    @DisplayName("lease 過期後新 owner 會取得更高 epoch")
    /**
     * 流程：owner-a 取得短 lease -> 時鐘推過 expiresAt -> owner-b acquire 成功且 epoch 遞增。
     */
    void expiredLeaseCanBeTakenOverWithHigherEpoch() {
        MutableClock clock = new MutableClock(Instant.parse("2026-05-27T00:00:00Z"));
        MatchingSequencerLeaseService service = new MatchingSequencerLeaseService(new InMemoryLeaseStore(), clock);

        MatchingSequencerLease ownerA = service.acquire("ETHUSDT", "owner-a", Duration.ofSeconds(1)).orElseThrow();
        clock.advance(Duration.ofSeconds(2));
        MatchingSequencerLease ownerB = service.acquire("ETHUSDT", "owner-b", Duration.ofSeconds(10)).orElseThrow();

        assertThat(ownerA.epoch()).isEqualTo(1L);
        assertThat(ownerB.ownerId()).isEqualTo("owner-b");
        assertThat(ownerB.epoch()).isEqualTo(2L);
    }

    @Test
    @DisplayName("requireWritable 會拒絕錯誤 owner、stale epoch 與過期 lease")
    /**
     * 流程：owner-a 取得 lease -> 驗證正確 owner/epoch 可寫 ->
     * 再分別驗證錯 owner、舊 epoch、過期 lease 都會被拒絕。
     */
    void requireWritableRejectsStaleOwnerEpochAndExpiredLease() {
        MutableClock clock = new MutableClock(Instant.parse("2026-05-27T00:00:00Z"));
        MatchingSequencerLeaseService service = new MatchingSequencerLeaseService(new InMemoryLeaseStore(), clock);
        MatchingSequencerLease lease = service.acquire("BTCUSDT", "owner-a", Duration.ofSeconds(2)).orElseThrow();

        assertThat(service.requireWritable("BTCUSDT", "owner-a", lease.epoch())).isEqualTo(lease);
        assertThatThrownBy(() -> service.requireWritable("BTCUSDT", "owner-b", lease.epoch()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("owner mismatch");
        assertThatThrownBy(() -> service.requireWritable("BTCUSDT", "owner-a", lease.epoch() - 1))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("stale epoch");

        clock.advance(Duration.ofSeconds(3));
        assertThatThrownBy(() -> service.requireWritable("BTCUSDT", "owner-a", lease.epoch()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("lease expired");
    }

    @Test
    @DisplayName("requireWritable 在無 lease 時拒絕寫入")
    /**
     * 流程：未 acquire 任何 lease -> 直接檢查 writable -> 驗證 command write guard 會拒絕。
     */
    void requireWritableRejectsMissingLease() {
        MatchingSequencerLeaseService service = new MatchingSequencerLeaseService(
                new InMemoryLeaseStore(),
                new MutableClock(Instant.parse("2026-05-27T00:00:00Z"))
        );

        assertThatThrownBy(() -> service.requireWritable("BTCUSDT", "owner-a", 1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("lease not found");
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
            MatchingSequencerLease current = leases.get(normalize(symbolCode));
            if (current == null
                    || !current.ownerId().equals(ownerId)
                    || current.epoch() != epoch
                    || !current.expiresAt().isAfter(now)) {
                return Optional.empty();
            }
            MatchingSequencerLease renewed = new MatchingSequencerLease(
                    current.symbolCode(),
                    ownerId,
                    epoch,
                    now.plus(ttl),
                    Math.max(0L, commandOffset),
                    Math.max(0L, eventOffset),
                    now
            );
            leases.put(current.symbolCode(), renewed);
            return Optional.of(renewed);
        }

        @Override
        public boolean release(String symbolCode, String ownerId, long epoch, Instant now) {
            MatchingSequencerLease current = leases.get(normalize(symbolCode));
            if (current == null || !current.ownerId().equals(ownerId) || current.epoch() != epoch) {
                return false;
            }
            leases.put(current.symbolCode(), new MatchingSequencerLease(
                    current.symbolCode(),
                    ownerId,
                    epoch,
                    now,
                    current.commandOffset(),
                    current.eventOffset(),
                    now
            ));
            return true;
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
