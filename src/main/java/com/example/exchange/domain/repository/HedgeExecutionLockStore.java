/*
 * 檔案用途：Repository contract，提供 market-maker hedge execution worker lock。
 */
package com.example.exchange.domain.repository;

import com.example.exchange.domain.model.dto.HedgeExecutionLock;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

public interface HedgeExecutionLockStore {

    Optional<HedgeExecutionLock> acquire(String lockName, String ownerId, Duration ttl, Instant now);

    boolean release(String lockName, String ownerId, Instant now);
}
