/*
 * 檔案用途：應用服務，提供 matching sequencer lease acquire / renew / release 操作。
 */
package com.example.exchange.application.service;

import com.example.exchange.domain.model.dto.MatchingSequencerLease;
import com.example.exchange.domain.repository.MatchingSequencerLeaseStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Matching sequencer lease application service。
 *
 * <p>此服務固定 worker ownership lifecycle。完整 command write fencing 仍需在 command/event
 * log 寫入時帶入 epoch 並拒絕 stale epoch。</p>
 */
@Service
public class MatchingSequencerLeaseService {

    private static final Duration DEFAULT_TTL = Duration.ofSeconds(30);

    private final MatchingSequencerLeaseStore leaseStore;
    private final Clock clock;

    @Autowired
    public MatchingSequencerLeaseService(MatchingSequencerLeaseStore leaseStore) {
        this(leaseStore, Clock.systemUTC());
    }

    MatchingSequencerLeaseService(MatchingSequencerLeaseStore leaseStore, Clock clock) {
        this.leaseStore = leaseStore;
        this.clock = clock;
    }

    /**
     * 取得 symbol ownership；若未過期 lease 屬於其他 owner，會回傳 empty。
     */
    public Optional<MatchingSequencerLease> acquire(String symbolCode, String ownerId) {
        return acquire(symbolCode, ownerId, DEFAULT_TTL);
    }

    /**
     * 取得 symbol ownership；ttl 由 caller 指定，便於測試與不同部署調整。
     */
    public Optional<MatchingSequencerLease> acquire(String symbolCode, String ownerId, Duration ttl) {
        return leaseStore.acquire(symbolCode, ownerId, ttl, Instant.now(clock));
    }

    /**
     * 續租現有 ownership，同時推進 owner 已處理的 command/event checkpoint。
     */
    public Optional<MatchingSequencerLease> renew(
            String symbolCode,
            String ownerId,
            long epoch,
            long commandOffset,
            long eventOffset
    ) {
        return renew(symbolCode, ownerId, epoch, DEFAULT_TTL, commandOffset, eventOffset);
    }

    /**
     * 續租現有 ownership；ttl 由 caller 指定，讓 worker 可使用 deployment 設定的 lease 期限。
     */
    public Optional<MatchingSequencerLease> renew(
            String symbolCode,
            String ownerId,
            long epoch,
            Duration ttl,
            long commandOffset,
            long eventOffset
    ) {
        return leaseStore.renew(
                symbolCode,
                ownerId,
                epoch,
                ttl,
                commandOffset,
                eventOffset,
                Instant.now(clock)
        );
    }

    /**
     * 釋放 ownership；ownerId 或 epoch 不一致時不會釋放。
     */
    public boolean release(String symbolCode, String ownerId, long epoch) {
        return leaseStore.release(symbolCode, ownerId, epoch, Instant.now(clock));
    }

    /**
     * 驗證目前 command writer 仍持有有效 ownership。
     *
     * <p>Production worker 在寫入 matching command/event 前應先呼叫此方法；ownerId、epoch
     * 或 expiry 任一不符合時會丟出例外，避免 stale owner 繼續處理 live command。</p>
     */
    public MatchingSequencerLease requireWritable(String symbolCode, String ownerId, long epoch) {
        MatchingSequencerLease lease = leaseStore.current(symbolCode)
                .orElseThrow(() -> new IllegalStateException("matching sequencer lease not found"));
        Instant now = Instant.now(clock);
        if (!lease.ownerId().equals(ownerId)) {
            throw new IllegalStateException("matching sequencer owner mismatch");
        }
        if (lease.epoch() != epoch) {
            throw new IllegalStateException("matching sequencer stale epoch");
        }
        if (!lease.expiresAt().isAfter(now)) {
            throw new IllegalStateException("matching sequencer lease expired");
        }
        return lease;
    }

    public Optional<MatchingSequencerLease> current(String symbolCode) {
        return leaseStore.current(symbolCode);
    }
}
