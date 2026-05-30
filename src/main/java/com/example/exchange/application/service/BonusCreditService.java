/*
 * 檔案用途：應用服務，管理體驗金 grant、消耗、到期與追回流程。
 */
package com.example.exchange.application.service;

import com.example.exchange.domain.model.dto.BonusCreditGrant;
import com.example.exchange.domain.model.dto.BonusCreditCampaignReport;
import com.example.exchange.domain.model.dto.BonusCreditReport;
import com.example.exchange.domain.repository.BonusCreditGrantStore;
import com.example.exchange.infra.config.BonusCreditProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class BonusCreditService {

    private final BonusCreditGrantStore grantStore;
    private final WalletLedgerService walletLedgerService;
    private final BonusCreditProperties properties;
    private final Clock clock;

    @Autowired
    public BonusCreditService(
            BonusCreditGrantStore grantStore,
            WalletLedgerService walletLedgerService,
            BonusCreditProperties properties
    ) {
        this(grantStore, walletLedgerService, properties, Clock.systemUTC());
    }

    BonusCreditService(BonusCreditGrantStore grantStore, WalletLedgerService walletLedgerService, Clock clock) {
        this(grantStore, walletLedgerService, new BonusCreditProperties(), clock);
    }

    BonusCreditService(
            BonusCreditGrantStore grantStore,
            WalletLedgerService walletLedgerService,
            BonusCreditProperties properties,
            Clock clock
    ) {
        this.grantStore = grantStore;
        this.walletLedgerService = walletLedgerService;
        this.properties = properties == null ? new BonusCreditProperties() : properties;
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
        return consume(uid, asset, amount, refId, expenseAccount, null, null);
    }

    public BigDecimal consume(
            long uid,
            String asset,
            BigDecimal amount,
            String refId,
            String expenseAccount,
            String symbol,
            String orderType
    ) {
        if (amount == null || amount.signum() <= 0) return BigDecimal.ZERO;
        if (!eligibleForConsumption(symbol, orderType, expenseAccount)) return BigDecimal.ZERO;
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

    public boolean eligibleForConsumption(String symbol, String orderType, String expenseAccount) {
        BonusCreditProperties.Eligibility eligibility = properties.getEligibility();
        if (eligibility == null || !eligibility.isEnabled()) return true;
        String normalizedSymbol = normalize(symbol);
        String normalizedOrderType = normalize(orderType);
        String normalizedExpenseAccount = normalize(expenseAccount);
        if (containsNormalized(eligibility.getBlockedSymbols(), normalizedSymbol)) return false;
        if (!allowedByList(eligibility.getAllowedSymbols(), normalizedSymbol)) return false;
        if (!allowedByList(eligibility.getAllowedOrderTypes(), normalizedOrderType)) return false;
        return allowedByList(eligibility.getAllowedExpenseAccounts(), normalizedExpenseAccount);
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

    public BigDecimal clawbackCampaign(String campaignId, String asset, BigDecimal amount, String refId) {
        if (campaignId == null || campaignId.isBlank()) return BigDecimal.ZERO;
        if (amount == null || amount.signum() <= 0) return BigDecimal.ZERO;
        String normalizedAsset = normalizeAsset(asset);
        if (normalizedAsset == null) return BigDecimal.ZERO;
        Instant now = clock.instant();
        BigDecimal remainingNeed = amount;
        BigDecimal totalClawedBack = BigDecimal.ZERO;
        List<BonusCreditGrant> grants = grantStore.findByCampaignId(campaignId).stream()
                .filter(grant -> BonusCreditGrant.ACTIVE.equals(grant.status()))
                .filter(grant -> normalizedAsset.equals(normalizeAsset(grant.asset())))
                .filter(grant -> grant.expiresAt() == null || grant.expiresAt().isAfter(now))
                .sorted(Comparator
                        .comparing(BonusCreditService::sortableExpiry)
                        .thenComparing(BonusCreditGrant::grantedAt)
                        .thenComparing(BonusCreditGrant::uid)
                        .thenComparing(BonusCreditGrant::id))
                .toList();
        for (BonusCreditGrant grant : grants) {
            if (remainingNeed.signum() <= 0) break;
            BigDecimal clawbackAmount = grant.remainingAmount().min(remainingNeed);
            if (clawbackAmount.signum() <= 0) continue;
            BigDecimal ledgerClawedBack = walletLedgerService.clawbackBonusCredit(
                    grant.uid(),
                    grant.asset(),
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

    public BonusCreditCampaignReport campaignReport(String campaignId, String asset) {
        Instant now = clock.instant();
        String normalizedAsset = normalizeAsset(asset);
        List<BonusCreditGrant> grants = grantStore.findByCampaignId(campaignId).stream()
                .filter(grant -> normalizedAsset == null || normalizedAsset.equals(normalizeAsset(grant.asset())))
                .sorted(Comparator
                        .comparing(BonusCreditGrant::grantedAt)
                        .thenComparing(BonusCreditGrant::uid)
                        .thenComparing(BonusCreditGrant::id))
                .toList();
        return new BonusCreditCampaignReport(
                campaignId,
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
                        .map(BonusCreditGrant::uid)
                        .collect(Collectors.toSet())
                        .size(),
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

    private static Instant sortableExpiry(BonusCreditGrant grant) {
        return grant.expiresAt() == null ? Instant.MAX : grant.expiresAt();
    }

    private static String normalizeAsset(String asset) {
        if (asset == null || asset.isBlank()) return null;
        return asset.trim().toUpperCase();
    }

    private static boolean allowedByList(List<String> allowedValues, String value) {
        return allowedValues == null
                || allowedValues.stream().map(BonusCreditService::normalize).allMatch(Objects::isNull)
                || containsNormalized(allowedValues, value);
    }

    private static boolean containsNormalized(List<String> values, String value) {
        if (values == null || values.isEmpty() || value == null) return false;
        return values.stream()
                .map(BonusCreditService::normalize)
                .anyMatch(value::equals);
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim().toUpperCase();
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
