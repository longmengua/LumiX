/*
 * 檔案用途：應用服務，編排領域模型、Repository 與外部基礎設施完成業務流程。
 */
package com.example.exchange.application.service;

import com.example.exchange.domain.model.dto.AdlQueueEntry;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class InsuranceFundService {

    private static final BigDecimal DEFAULT_BALANCE = new BigDecimal("1000000");

    private final Map<String, BigDecimal> balances = new ConcurrentHashMap<>();
    private final List<AdlQueueEntry> adlQueue = new ArrayList<>();

    public synchronized BigDecimal balance(String asset) {
        return balances.computeIfAbsent(normalize(asset), ignored -> DEFAULT_BALANCE);
    }

    public synchronized BigDecimal cover(String asset, BigDecimal requested) {
        if (requested == null || requested.signum() <= 0) return BigDecimal.ZERO;
        String key = normalize(asset);
        BigDecimal current = balance(key);
        BigDecimal covered = current.min(requested);
        balances.put(key, current.subtract(covered));
        return covered;
    }

    public synchronized void enqueueAdl(
            String liquidationId,
            long uid,
            String symbol,
            BigDecimal amount
    ) {
        enqueueAdl(liquidationId, uid, symbol, null, amount);
    }

    public synchronized void enqueueAdl(
            String liquidationId,
            long uid,
            String symbol,
            String liquidatedSide,
            BigDecimal amount
    ) {
        if (amount == null || amount.signum() <= 0) return;
        if (hasAdlEntry(liquidationId)) return;
        adlQueue.add(new AdlQueueEntry(
                liquidationId,
                uid,
                normalize(symbol),
                normalizeSide(liquidatedSide),
                amount,
                Instant.now(),
                "OPEN",
                "",
                null
        ));
    }

    public synchronized List<AdlQueueEntry> adlQueue() {
        return List.copyOf(adlQueue);
    }

    public synchronized boolean completeAdl(String liquidationId) {
        return adlQueue.removeIf(entry -> entry.liquidationId().equals(liquidationId));
    }

    public synchronized AdlQueueEntry updateAdlRemaining(String liquidationId, BigDecimal remainingAmount) {
        if (remainingAmount == null || remainingAmount.signum() <= 0) {
            completeAdl(liquidationId);
            return null;
        }
        int index = indexOf(liquidationId);
        AdlQueueEntry current = adlQueue.get(index);
        AdlQueueEntry updated = new AdlQueueEntry(
                current.liquidationId(),
                current.uid(),
                current.symbol(),
                current.liquidatedSide(),
                remainingAmount,
                current.ts(),
                current.status(),
                current.owner(),
                current.claimedAt()
        );
        adlQueue.set(index, updated);
        return updated;
    }

    public synchronized AdlQueueEntry claimAdl(String liquidationId, String owner) {
        String normalizedOwner = requireOwner(owner);
        int index = indexOf(liquidationId);
        AdlQueueEntry current = adlQueue.get(index);
        if ("CLAIMED".equals(current.status()) && !normalizedOwner.equals(current.owner())) {
            throw new IllegalStateException("ADL queue entry already claimed by " + current.owner());
        }
        AdlQueueEntry claimed = copy(current, "CLAIMED", normalizedOwner, Instant.now());
        adlQueue.set(index, claimed);
        return claimed;
    }

    public synchronized AdlQueueEntry releaseAdl(String liquidationId, String owner) {
        String normalizedOwner = requireOwner(owner);
        int index = indexOf(liquidationId);
        AdlQueueEntry current = adlQueue.get(index);
        if ("CLAIMED".equals(current.status()) && !normalizedOwner.equals(current.owner())) {
            throw new IllegalStateException("ADL queue entry claimed by " + current.owner());
        }
        AdlQueueEntry released = copy(current, "OPEN", "", null);
        adlQueue.set(index, released);
        return released;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }

    private static String normalizeSide(String value) {
        String normalized = normalize(value);
        return ("LONG".equals(normalized) || "SHORT".equals(normalized)) ? normalized : "";
    }

    private int indexOf(String liquidationId) {
        if (liquidationId == null || liquidationId.isBlank()) {
            throw new IllegalArgumentException("liquidationId must not be blank");
        }
        for (int i = 0; i < adlQueue.size(); i++) {
            if (liquidationId.trim().equals(adlQueue.get(i).liquidationId())) {
                return i;
            }
        }
        throw new IllegalArgumentException("ADL queue entry not found: " + liquidationId);
    }

    private boolean hasAdlEntry(String liquidationId) {
        if (liquidationId == null || liquidationId.isBlank()) {
            throw new IllegalArgumentException("liquidationId must not be blank");
        }
        return adlQueue.stream()
                .anyMatch(entry -> liquidationId.trim().equals(entry.liquidationId()));
    }

    private static String requireOwner(String owner) {
        if (owner == null || owner.isBlank()) {
            throw new IllegalArgumentException("ADL queue owner must not be blank");
        }
        return owner.trim();
    }

    private static AdlQueueEntry copy(AdlQueueEntry current, String status, String owner, Instant claimedAt) {
        return new AdlQueueEntry(
                current.liquidationId(),
                current.uid(),
                current.symbol(),
                current.liquidatedSide(),
                current.amount(),
                current.ts(),
                status,
                owner,
                claimedAt
        );
    }
}
