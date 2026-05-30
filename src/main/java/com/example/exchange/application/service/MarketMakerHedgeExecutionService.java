/*
 * 檔案用途：應用服務，串接 exposure、reduce-only strategy 與 hedging venue routing。
 */
package com.example.exchange.application.service;

import com.example.exchange.domain.model.dto.HedgeDecision;
import com.example.exchange.domain.model.dto.HedgeExecutionLock;
import com.example.exchange.domain.model.dto.HedgeExecutionReport;
import com.example.exchange.domain.model.dto.HedgeStrategyDecision;
import com.example.exchange.domain.model.dto.MarketMakerExposure;
import com.example.exchange.domain.model.dto.MarketMakerProfile;
import com.example.exchange.domain.repository.HedgeExecutionLockStore;
import com.example.exchange.infra.config.RiskControlsProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MarketMakerHedgeExecutionService {

    private final MarketMakerProfileService profileService;
    private final MarketMakerExposureService exposureService;
    private final MarketMakerHedgeStrategyService strategyService;
    private final MarketMakerHedgingService hedgingService;
    private final RiskControlsProperties riskControlsProperties;
    private CommandTransactionBoundary commandTransactionBoundary;
    private HedgeExecutionLockStore lockStore;

    @Value("${market-maker.hedge-execution.lock-enabled:false}")
    private boolean lockEnabled;

    @Value("${market-maker.hedge-execution.lock-owner-id:${HOSTNAME:unknown-hedge-worker}}")
    private String lockOwnerId;

    @Value("${market-maker.hedge-execution.lock-ttl-ms:30000}")
    private long lockTtlMs;

    @Autowired(required = false)
    public void setCommandTransactionBoundary(CommandTransactionBoundary commandTransactionBoundary) {
        this.commandTransactionBoundary = commandTransactionBoundary;
    }

    @Autowired(required = false)
    public void setLockStore(HedgeExecutionLockStore lockStore) {
        this.lockStore = lockStore;
    }

    void configureWorkerLockForTest(boolean enabled, String ownerId, long ttlMs) {
        this.lockEnabled = enabled;
        this.lockOwnerId = ownerId;
        this.lockTtlMs = ttlMs;
    }

    @Transactional
    public HedgeExecutionReport executeForMarketMaker(String marketMakerId, String refPrefix) {
        if (commandTransactionBoundary != null) {
            return commandTransactionBoundary.execute(
                    "market-maker-hedge-execution",
                    () -> executeForMarketMakerInsideTransaction(marketMakerId, refPrefix)
            );
        }
        return executeForMarketMakerInsideTransaction(marketMakerId, refPrefix);
    }

    private HedgeExecutionReport executeForMarketMakerInsideTransaction(String marketMakerId, String refPrefix) {
        MarketMakerProfile profile = profileService.findByMarketMakerId(marketMakerId)
                .orElseThrow(() -> new IllegalArgumentException("market maker profile not found"));
        return execute(profile, refPrefix);
    }

    @Transactional
    public List<HedgeExecutionReport> executeForEnabledMarketMakers(String refPrefix) {
        if (commandTransactionBoundary != null) {
            return commandTransactionBoundary.execute(
                    "market-maker-enabled-hedge-execution",
                    () -> executeForEnabledMarketMakersInsideTransaction(refPrefix)
            );
        }
        return executeForEnabledMarketMakersInsideTransaction(refPrefix);
    }

    private List<HedgeExecutionReport> executeForEnabledMarketMakersInsideTransaction(String refPrefix) {
        Optional<HedgeExecutionLock> lock = acquireBatchLock();
        if (lockEnabled && lock.isEmpty()) {
            return List.of();
        }
        try {
            return profileService.enabledProfiles().stream()
                    .map(profile -> execute(profile, refPrefix))
                    .toList();
        } finally {
            lock.ifPresent(ignored -> lockStore.release(batchLockName(), lockOwnerId, Instant.now()));
        }
    }

    private HedgeExecutionReport execute(MarketMakerProfile profile, String refPrefix) {
        List<MarketMakerExposure> exposures = exposureService.exposures(profile);
        if (riskControlsProperties.isMarketMakerHedgeExecutionHalt()) {
            return haltedReport(profile, exposures);
        }
        List<HedgeStrategyDecision> strategyDecisions = new ArrayList<>();
        List<HedgeDecision> hedgeDecisions = new ArrayList<>();
        for (MarketMakerExposure exposure : exposures) {
            HedgeStrategyDecision strategyDecision = strategyService.planReduceOnlyHedge(
                    profile,
                    exposure,
                    refId(refPrefix, profile.marketMakerId(), exposure.symbol())
            );
            if (strategyDecision.hedgeRequired()) {
                if (isPolicyRouteLimitReached(hedgeDecisions.size())) {
                    strategyDecisions.add(policyRejectedDecision(profile, exposure));
                    continue;
                }
                hedgeDecisions.add(hedgingService.hedge(profile, strategyDecision.orderRequest()));
            }
            strategyDecisions.add(strategyDecision);
        }
        return new HedgeExecutionReport(
                profile.marketMakerId(),
                exposures.size(),
                (int) strategyDecisions.stream().filter(HedgeStrategyDecision::hedgeRequired).count(),
                hedgeDecisions.size(),
                Instant.now(),
                strategyDecisions,
                hedgeDecisions
        );
    }

    private boolean isPolicyRouteLimitReached(int routedCount) {
        RiskControlsProperties.MarketMakerHedgeExecutionPolicy policy =
                riskControlsProperties.getMarketMakerHedgeExecutionPolicy();
        if (policy == null || !policy.isEnabled() || policy.getMaxRoutedOrdersPerRun() <= 0) {
            return false;
        }
        return routedCount >= policy.getMaxRoutedOrdersPerRun();
    }

    private Optional<HedgeExecutionLock> acquireBatchLock() {
        if (!lockEnabled) {
            return Optional.empty();
        }
        if (lockStore == null) {
            throw new IllegalStateException("hedge execution lock enabled but store is not configured");
        }
        return lockStore.acquire(
                batchLockName(),
                lockOwnerId,
                Duration.ofMillis(Math.max(1, lockTtlMs)),
                Instant.now()
        );
    }

    private static String batchLockName() {
        return "market-maker-hedge-execution";
    }

    private static HedgeStrategyDecision policyRejectedDecision(
            MarketMakerProfile profile,
            MarketMakerExposure exposure
    ) {
        return new HedgeStrategyDecision(
                profile.marketMakerId(),
                exposure.symbol(),
                false,
                "HEDGE_EXECUTION_POLICY_MAX_ORDERS",
                exposure,
                null
        );
    }

    private static HedgeExecutionReport haltedReport(MarketMakerProfile profile, List<MarketMakerExposure> exposures) {
        List<HedgeStrategyDecision> decisions = exposures.stream()
                .map(exposure -> new HedgeStrategyDecision(
                        profile.marketMakerId(),
                        exposure.symbol(),
                        false,
                        "HEDGE_EXECUTION_HALTED",
                        exposure,
                        null
                ))
                .toList();
        return new HedgeExecutionReport(
                profile.marketMakerId(),
                exposures.size(),
                0,
                0,
                Instant.now(),
                decisions,
                List.of()
        );
    }

    private static String refId(String refPrefix, String marketMakerId, String symbol) {
        String normalizedPrefix = refPrefix == null || refPrefix.isBlank() ? "inventory-hedge" : refPrefix.trim();
        return normalizedPrefix + ":" + marketMakerId + ":" + symbol + ":" + Instant.now().toEpochMilli();
    }
}
