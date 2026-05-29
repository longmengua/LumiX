/*
 * 檔案用途：應用服務，編排領域模型、Repository 與外部基礎設施完成業務流程。
 */
package com.example.exchange.application.service;

import com.example.exchange.domain.model.dto.AdlQueueEntry;
import com.example.exchange.domain.repository.AdlQueueStore;
import com.example.exchange.domain.repository.InMemoryAdlQueueStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class InsuranceFundService {

    private static final BigDecimal DEFAULT_BALANCE = new BigDecimal("1000000");

    private final Map<String, BigDecimal> balances = new ConcurrentHashMap<>();
    private final AdlQueueStore adlQueueStore;

    public InsuranceFundService() {
        this(new InMemoryAdlQueueStore());
    }

    @Autowired
    public InsuranceFundService(AdlQueueStore adlQueueStore) {
        this.adlQueueStore = adlQueueStore;
    }

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
        adlQueueStore.enqueueIfAbsent(new AdlQueueEntry(
                requireLiquidationId(liquidationId),
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
        return adlQueueStore.list();
    }

    public synchronized List<AdlQueueEntry> stuckAdlClaims(Duration minClaimAge) {
        Duration threshold = minClaimAge == null || minClaimAge.isNegative()
                ? Duration.ZERO
                : minClaimAge;
        Instant cutoff = Instant.now().minus(threshold);
        return adlQueueStore.list().stream()
                .filter(entry -> "CLAIMED".equals(entry.status()))
                .filter(entry -> entry.claimedAt() != null && !entry.claimedAt().isAfter(cutoff))
                .toList();
    }

    public synchronized boolean completeAdl(String liquidationId) {
        return adlQueueStore.complete(liquidationId);
    }

    public synchronized AdlQueueEntry updateAdlRemaining(String liquidationId, BigDecimal remainingAmount) {
        return adlQueueStore.updateRemaining(liquidationId, remainingAmount);
    }

    public synchronized AdlQueueEntry claimAdl(String liquidationId, String owner) {
        return adlQueueStore.claim(liquidationId, owner);
    }

    public synchronized AdlQueueEntry releaseAdl(String liquidationId, String owner) {
        return adlQueueStore.release(liquidationId, owner);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }

    private static String normalizeSide(String value) {
        String normalized = normalize(value);
        return ("LONG".equals(normalized) || "SHORT".equals(normalized)) ? normalized : "";
    }

    private static String requireLiquidationId(String liquidationId) {
        if (liquidationId == null || liquidationId.isBlank()) {
            throw new IllegalArgumentException("liquidationId must not be blank");
        }
        return liquidationId.trim();
    }
}
