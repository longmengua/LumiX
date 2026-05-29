/*
 * 檔案用途：ADL queue store 的 in-memory fallback，供單元測試與未接資料庫的本地流程使用。
 */
package com.example.exchange.domain.repository;

import com.example.exchange.domain.model.dto.AdlQueueEntry;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class InMemoryAdlQueueStore implements AdlQueueStore {

    private final List<AdlQueueEntry> queue = new ArrayList<>();

    @Override
    public synchronized boolean enqueueIfAbsent(AdlQueueEntry entry) {
        requireLiquidationId(entry.liquidationId());
        if (indexOfOrMinusOne(entry.liquidationId()) >= 0) {
            return false;
        }
        queue.add(entry);
        return true;
    }

    @Override
    public synchronized List<AdlQueueEntry> list() {
        return List.copyOf(queue);
    }

    @Override
    public synchronized boolean complete(String liquidationId) {
        requireLiquidationId(liquidationId);
        return queue.removeIf(entry -> liquidationId.trim().equals(entry.liquidationId()));
    }

    @Override
    public synchronized AdlQueueEntry updateRemaining(String liquidationId, BigDecimal remainingAmount) {
        if (remainingAmount == null || remainingAmount.signum() <= 0) {
            complete(liquidationId);
            return null;
        }
        int index = indexOf(liquidationId);
        AdlQueueEntry current = queue.get(index);
        AdlQueueEntry updated = copy(current, remainingAmount, current.status(), current.owner(), current.claimedAt());
        queue.set(index, updated);
        return updated;
    }

    @Override
    public synchronized AdlQueueEntry claim(String liquidationId, String owner) {
        String normalizedOwner = requireOwner(owner);
        int index = indexOf(liquidationId);
        AdlQueueEntry current = queue.get(index);
        if ("CLAIMED".equals(current.status()) && !normalizedOwner.equals(current.owner())) {
            throw new IllegalStateException("ADL queue entry already claimed by " + current.owner());
        }
        AdlQueueEntry claimed = copy(current, current.amount(), "CLAIMED", normalizedOwner, Instant.now());
        queue.set(index, claimed);
        return claimed;
    }

    @Override
    public synchronized AdlQueueEntry release(String liquidationId, String owner) {
        String normalizedOwner = requireOwner(owner);
        int index = indexOf(liquidationId);
        AdlQueueEntry current = queue.get(index);
        if ("CLAIMED".equals(current.status()) && !normalizedOwner.equals(current.owner())) {
            throw new IllegalStateException("ADL queue entry claimed by " + current.owner());
        }
        AdlQueueEntry released = copy(current, current.amount(), "OPEN", "", null);
        queue.set(index, released);
        return released;
    }

    private int indexOf(String liquidationId) {
        int index = indexOfOrMinusOne(liquidationId);
        if (index < 0) {
            throw new IllegalArgumentException("ADL queue entry not found: " + liquidationId);
        }
        return index;
    }

    private int indexOfOrMinusOne(String liquidationId) {
        requireLiquidationId(liquidationId);
        String normalized = liquidationId.trim();
        for (int i = 0; i < queue.size(); i++) {
            if (normalized.equals(queue.get(i).liquidationId())) {
                return i;
            }
        }
        return -1;
    }

    private static void requireLiquidationId(String liquidationId) {
        if (liquidationId == null || liquidationId.isBlank()) {
            throw new IllegalArgumentException("liquidationId must not be blank");
        }
    }

    private static String requireOwner(String owner) {
        if (owner == null || owner.isBlank()) {
            throw new IllegalArgumentException("ADL queue owner must not be blank");
        }
        return owner.trim();
    }

    private static AdlQueueEntry copy(
            AdlQueueEntry current,
            BigDecimal amount,
            String status,
            String owner,
            Instant claimedAt
    ) {
        return new AdlQueueEntry(
                current.liquidationId(),
                current.uid(),
                current.symbol(),
                current.liquidatedSide(),
                amount,
                current.ts(),
                status,
                owner,
                claimedAt
        );
    }
}
