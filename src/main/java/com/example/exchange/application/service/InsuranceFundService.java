/*
 * 檔案用途：應用服務，編排領域模型、Repository 與外部基礎設施完成業務流程。
 */
package com.example.exchange.application.service;

import com.example.exchange.domain.model.dto.AdlOperationalAlert;
import com.example.exchange.domain.model.dto.AdlOperationalAlertReport;
import com.example.exchange.domain.model.dto.AdlQueueEntry;
import com.example.exchange.domain.model.dto.InsuranceFundMovement;
import com.example.exchange.domain.repository.AdlQueueStore;
import com.example.exchange.domain.repository.InMemoryAdlQueueStore;
import com.example.exchange.domain.repository.InMemoryInsuranceFundMovementStore;
import com.example.exchange.domain.repository.InsuranceFundMovementStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class InsuranceFundService {

    private static final BigDecimal DEFAULT_BALANCE = new BigDecimal("1000000");

    private final Map<String, BigDecimal> balances = new ConcurrentHashMap<>();
    private final AdlQueueStore adlQueueStore;
    private final InsuranceFundMovementStore movementStore;

    public InsuranceFundService() {
        this(new InMemoryAdlQueueStore(), new InMemoryInsuranceFundMovementStore());
    }

    public InsuranceFundService(AdlQueueStore adlQueueStore) {
        this(adlQueueStore, new InMemoryInsuranceFundMovementStore());
    }

    @Autowired
    public InsuranceFundService(AdlQueueStore adlQueueStore, InsuranceFundMovementStore movementStore) {
        this.adlQueueStore = adlQueueStore;
        this.movementStore = movementStore;
    }

    public synchronized BigDecimal balance(String asset) {
        return balances.computeIfAbsent(normalize(asset), ignored -> DEFAULT_BALANCE);
    }

    public synchronized BigDecimal cover(String asset, BigDecimal requested) {
        return cover(asset, requested, "");
    }

    public synchronized BigDecimal cover(String asset, BigDecimal requested, String refId) {
        if (requested == null || requested.signum() <= 0) return BigDecimal.ZERO;
        String key = normalize(asset);
        BigDecimal current = balance(key);
        BigDecimal covered = current.min(requested);
        BigDecimal balanceAfter = current.subtract(covered);
        balances.put(key, balanceAfter);
        if (covered.signum() > 0) {
            movementStore.save(new InsuranceFundMovement(
                    "ifm-" + UUID.randomUUID(),
                    key,
                    "INSURANCE_FUND_PAYOUT",
                    refId == null ? "" : refId.trim(),
                    covered.negate(),
                    balanceAfter,
                    Instant.now()
            ));
        }
        return covered;
    }

    public synchronized List<InsuranceFundMovement> movements(String asset, int limit) {
        return movementStore.findRecent(asset, limit);
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

    public synchronized AdlOperationalAlertReport adlOperationalAlerts(Duration minAge) {
        Duration threshold = minAge == null || minAge.isNegative()
                ? Duration.ZERO
                : minAge;
        Instant now = Instant.now();
        Instant cutoff = now.minus(threshold);
        List<AdlOperationalAlert> alerts = new ArrayList<>();
        for (AdlQueueEntry entry : adlQueueStore.list()) {
            if ("CLAIMED".equals(entry.status())
                    && entry.claimedAt() != null
                    && !entry.claimedAt().isAfter(cutoff)) {
                alerts.add(alert(
                        "ADL_CLAIM_STUCK",
                        "CRITICAL",
                        entry,
                        Duration.between(entry.claimedAt(), now).toSeconds(),
                        "claimed ADL queue entry exceeded operator claim age threshold"
                ));
            } else if ("OPEN".equals(entry.status())
                    && entry.ts() != null
                    && !entry.ts().isAfter(cutoff)) {
                alerts.add(alert(
                        "ADL_QUEUE_OPEN_OVER_THRESHOLD",
                        "WARNING",
                        entry,
                        Duration.between(entry.ts(), now).toSeconds(),
                        "open ADL queue entry exceeded retry/assignment age threshold"
                ));
            }
        }
        return new AdlOperationalAlertReport(alerts.size(), now, alerts);
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

    private static AdlOperationalAlert alert(
            String type,
            String severity,
            AdlQueueEntry entry,
            long ageSeconds,
            String detail
    ) {
        return new AdlOperationalAlert(
                type,
                severity,
                entry.liquidationId(),
                entry.uid(),
                entry.symbol(),
                entry.amount(),
                entry.status(),
                entry.owner(),
                Math.max(0, ageSeconds),
                detail
        );
    }
}
