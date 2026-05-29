/*
 * 檔案用途：應用服務，管理體驗金 grant、消耗、到期與追回流程。
 */
package com.example.exchange.application.service;

import com.example.exchange.domain.model.dto.BonusCreditGrant;
import com.example.exchange.domain.model.dto.BonusCreditReport;
import com.example.exchange.domain.repository.BonusCreditGrantStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
public class BonusCreditService {

    private final BonusCreditGrantStore grantStore;
    private final WalletLedgerService walletLedgerService;
    private final Clock clock;

    @Autowired
    public BonusCreditService(BonusCreditGrantStore grantStore, WalletLedgerService walletLedgerService) {
        this(grantStore, walletLedgerService, Clock.systemUTC());
    }

    BonusCreditService(BonusCreditGrantStore grantStore, WalletLedgerService walletLedgerService, Clock clock) {
        this.grantStore = grantStore;
        this.walletLedgerService = walletLedgerService;
        this.clock = clock;
    }

    public BonusCreditGrant grant(
            long uid,
            String asset,
            BigDecimal amount,
            String campaignId,
            Instant expiresAt,
            String refId
    ) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("bonus amount must be positive");
        }
        Instant now = clock.instant();
        BonusCreditGrant grant = new BonusCreditGrant(
                null,
                uid,
                asset,
                amount,
                amount,
                campaignId,
                BonusCreditGrant.ACTIVE,
                now,
                expiresAt,
                now
        );
        walletLedgerService.grantBonusCredit(uid, asset, amount, refId);
        grantStore.save(grant);
        return grant;
    }

    public BigDecimal consume(long uid, String asset, BigDecimal amount, String refId, String expenseAccount) {
        if (amount == null || amount.signum() <= 0) return BigDecimal.ZERO;
        Instant now = clock.instant();
        BigDecimal remainingNeed = amount;
        BigDecimal totalConsumed = BigDecimal.ZERO;
        List<BonusCreditGrant> activeGrants = grantStore.findActiveByUidAndAsset(uid, asset);
        for (BonusCreditGrant grant : activeGrants) {
            if (remainingNeed.signum() <= 0) break;
            if (grant.expiresAt() != null && !grant.expiresAt().isAfter(now)) continue;
            BigDecimal consume = grant.remainingAmount().min(remainingNeed);
            if (consume.signum() <= 0) continue;
            // Ledger consume is written once per batch so refId can trace the exact campaign grant.
            BigDecimal ledgerConsumed = walletLedgerService.consumeBonusCredit(
                    uid,
                    asset,
                    consume,
                    consumeRef(refId, grant),
                    expenseAccount
            );
            if (ledgerConsumed.signum() <= 0) continue;
            totalConsumed = totalConsumed.add(ledgerConsumed);
            remainingNeed = remainingNeed.subtract(ledgerConsumed);
            grantStore.save(grant.withRemaining(grant.remainingAmount().subtract(ledgerConsumed), now));
        }
        return totalConsumed;
    }

    public int expireDue(int limit) {
        Instant now = clock.instant();
        int expired = 0;
        for (BonusCreditGrant grant : grantStore.findActiveExpiringAtOrBefore(now, limit)) {
            if (grant.remainingAmount().signum() <= 0) continue;
            BigDecimal ledgerExpired = walletLedgerService.expireBonusCredit(
                    grant.uid(),
                    grant.asset(),
                    grant.remainingAmount(),
                    "bonus-expire:" + grant.id()
            );
            if (ledgerExpired.signum() > 0) {
                grantStore.save(grant.expire(now));
                expired++;
            }
        }
        return expired;
    }

    public BigDecimal clawback(long uid, String asset, BigDecimal amount, String refId) {
        if (amount == null || amount.signum() <= 0) return BigDecimal.ZERO;
        Instant now = clock.instant();
        BigDecimal remainingNeed = amount;
        BigDecimal totalClawedBack = BigDecimal.ZERO;
        for (BonusCreditGrant grant : grantStore.findActiveByUidAndAsset(uid, asset)) {
            if (remainingNeed.signum() <= 0) break;
            if (grant.expiresAt() != null && !grant.expiresAt().isAfter(now)) continue;
            BigDecimal clawbackAmount = grant.remainingAmount().min(remainingNeed);
            if (clawbackAmount.signum() <= 0) continue;
            BigDecimal ledgerClawedBack = walletLedgerService.clawbackBonusCredit(
                    uid,
                    asset,
                    clawbackAmount,
                    clawbackRef(refId, grant)
            );
            if (ledgerClawedBack.signum() <= 0) continue;
            totalClawedBack = totalClawedBack.add(ledgerClawedBack);
            remainingNeed = remainingNeed.subtract(ledgerClawedBack);
            BigDecimal nextRemaining = grant.remainingAmount().subtract(ledgerClawedBack);
            grantStore.save(nextRemaining.signum() <= 0
                    ? grant.clawBack(now)
                    : grant.withRemaining(nextRemaining, now));
        }
        return totalClawedBack;
    }

    public BonusCreditReport report(long uid, String asset) {
        Instant now = clock.instant();
        String normalizedAsset = normalizeAsset(asset);
        List<BonusCreditGrant> grants = grantStore.findByUid(uid).stream()
                .filter(grant -> normalizedAsset == null || normalizedAsset.equals(normalizeAsset(grant.asset())))
                .sorted(Comparator
                        .comparing(BonusCreditGrant::grantedAt)
                        .thenComparing(BonusCreditGrant::id))
                .toList();
        return new BonusCreditReport(
                uid,
                normalizedAsset == null ? "ALL" : normalizedAsset,
                sum(grants, BonusCreditGrant::originalAmount),
                sum(grants, BonusCreditGrant::remainingAmount),
                sumByStatus(grants, BonusCreditGrant.ACTIVE),
                sumByStatus(grants, BonusCreditGrant.CONSUMED),
                sumByStatus(grants, BonusCreditGrant.EXPIRED),
                sumByStatus(grants, BonusCreditGrant.CLAWED_BACK),
                countByStatus(grants, BonusCreditGrant.ACTIVE),
                countByStatus(grants, BonusCreditGrant.CONSUMED),
                countByStatus(grants, BonusCreditGrant.EXPIRED),
                countByStatus(grants, BonusCreditGrant.CLAWED_BACK),
                grants.stream()
                        .filter(grant -> BonusCreditGrant.ACTIVE.equals(grant.status()))
                        .map(BonusCreditGrant::expiresAt)
                        .filter(Objects::nonNull)
                        .filter(expiresAt -> expiresAt.isAfter(now))
                        .min(Instant::compareTo)
                        .orElse(null),
                now,
                grants
        );
    }

    private static String consumeRef(String refId, BonusCreditGrant grant) {
        String prefix = refId == null || refId.isBlank() ? "bonus-consume" : refId;
        return prefix + ":" + grant.id();
    }

    private static String clawbackRef(String refId, BonusCreditGrant grant) {
        String prefix = refId == null || refId.isBlank() ? "bonus-clawback" : refId;
        return prefix + ":" + grant.id();
    }

    private static String normalizeAsset(String asset) {
        if (asset == null || asset.isBlank()) return null;
        return asset.trim().toUpperCase();
    }

    private static BigDecimal sum(
            List<BonusCreditGrant> grants,
            java.util.function.Function<BonusCreditGrant, BigDecimal> mapper
    ) {
        return grants.stream()
                .map(mapper)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal sumByStatus(List<BonusCreditGrant> grants, String status) {
        return grants.stream()
                .filter(grant -> status.equals(grant.status()))
                .map(BonusCreditGrant::originalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static int countByStatus(List<BonusCreditGrant> grants, String status) {
        return (int) grants.stream()
                .filter(grant -> status.equals(grant.status()))
                .count();
    }
}
