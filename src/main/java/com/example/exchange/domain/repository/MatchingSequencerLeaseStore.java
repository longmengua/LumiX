/*
 * 檔案用途：Repository contract，定義 matching sequencer lease / epoch fencing 基線。
 */
package com.example.exchange.domain.repository;

import com.example.exchange.domain.model.dto.MatchingSequencerLease;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Per-symbol matching sequencer lease store。
 *
 * <p>完整 production fencing 需要 command/event writes 也帶 epoch 並拒絕 stale owner；
 * 此 contract 先固定 owner lease 的 acquire、renew、release 與查詢語義。</p>
 */
public interface MatchingSequencerLeaseStore {

    Optional<MatchingSequencerLease> acquire(String symbolCode, String ownerId, Duration ttl, Instant now);

    Optional<MatchingSequencerLease> renew(
            String symbolCode,
            String ownerId,
            long epoch,
            Duration ttl,
            long commandOffset,
            long eventOffset,
            Instant now
    );

    boolean release(String symbolCode, String ownerId, long epoch, Instant now);

    Optional<MatchingSequencerLease> current(String symbolCode);
}
