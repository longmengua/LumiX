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

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MarketMakerHedgeExecutionService {

    private static final int MAX_REF_PREFIX_LENGTH = 64;
    private static final String REF_PREFIX_PATTERN = "[A-Za-z0-9._:-]+";

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

    @Value("${market-maker.hedge-execution.approval-required:false}")
    private boolean approvalRequired;

    @Value("${market-maker.hedge-execution.approval-token:}")
    private String approvalToken;

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

    void configureApprovalForTest(boolean required, String token) {
        this.approvalRequired = required;
        this.approvalToken = token;
    }

    @Transactional
    public HedgeExecutionReport executeForMarketMaker(String marketMakerId, String refPrefix) {
        return executeForMarketMaker(marketMakerId, refPrefix, null);
    }

    @Transactional
    public HedgeExecutionReport executeForMarketMaker(String marketMakerId, String refPrefix, String operatorApprovalToken) {
        requireApproval(operatorApprovalToken);
        validateRefPrefix(refPrefix);
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
        return execute(profile, refPrefix, new ExecutionBudget());
    }

    @Transactional
    public List<HedgeExecutionReport> executeForEnabledMarketMakers(String refPrefix) {
        return executeForEnabledMarketMakers(refPrefix, null);
    }

    @Transactional
    public List<HedgeExecutionReport> executeForEnabledMarketMakers(String refPrefix, String operatorApprovalToken) {
        requireApproval(operatorApprovalToken);
        validateRefPrefix(refPrefix);
        if (commandTransactionBoundary != null) {
            return commandTransactionBoundary.execute(
                    "market-maker-enabled-hedge-execution",
                    () -> executeForEnabledMarketMakersInsideTransaction(refPrefix)
            );
        }
        return executeForEnabledMarketMakersInsideTransaction(refPrefix);
    }

    private void requireApproval(String operatorApprovalToken) {
        if (!approvalRequired) {
            return;
        }
        if (approvalToken == null || approvalToken.isBlank()) {
            throw new IllegalStateException("hedge execution approval required but approval token is not configured");
        }
        if (operatorApprovalToken == null || !approvalToken.trim().equals(operatorApprovalToken.trim())) {
            throw new IllegalStateException("hedge execution operator approval required");
        }
    }

    private static void validateRefPrefix(String refPrefix) {
        if (refPrefix == null || refPrefix.isBlank()) {
            return;
        }
        String normalized = refPrefix.trim();
        if (normalized.length() > MAX_REF_PREFIX_LENGTH) {
            throw new IllegalArgumentException("hedge execution ref prefix is too long");
        }
        if (!normalized.matches(REF_PREFIX_PATTERN)) {
            throw new IllegalArgumentException("hedge execution ref prefix contains invalid characters");
        }
    }

    private List<HedgeExecutionReport> executeForEnabledMarketMakersInsideTransaction(String refPrefix) {
        Optional<HedgeExecutionLock> lock = acquireBatchLock();
        if (lockEnabled && lock.isEmpty()) {
            return List.of();
        }
        try {
            ExecutionBudget budget = new ExecutionBudget();
            return profileService.enabledProfiles().stream()
                    .map(profile -> execute(profile, refPrefix, budget))
                    .toList();
        } finally {
            lock.ifPresent(ignored -> lockStore.release(batchLockName(), lockOwnerId, Instant.now()));
        }
    }

    private HedgeExecutionReport execute(MarketMakerProfile profile, String refPrefix, ExecutionBudget budget) {
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
                String policyRejection = budget.rejectionFor(strategyDecision);
                if (policyRejection != null) {
                    strategyDecisions.add(policyRejectedDecision(profile, exposure, policyRejection));
                    continue;
                }
                hedgeDecisions.add(hedgingService.hedge(profile, strategyDecision.orderRequest()));
                budget.record(strategyDecision);
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
            MarketMakerExposure exposure,
            String reason
    ) {
        return new HedgeStrategyDecision(
                profile.marketMakerId(),
                exposure.symbol(),
                false,
                reason,
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

    private final class ExecutionBudget {
        private int routedCount;
        private BigDecimal routedNotional = BigDecimal.ZERO;

        private String rejectionFor(HedgeStrategyDecision decision) {
            RiskControlsProperties.MarketMakerHedgeExecutionPolicy policy =
                    riskControlsProperties.getMarketMakerHedgeExecutionPolicy();
            if (policy == null || !policy.isEnabled()) {
                return null;
            }
            if (policy.getMaxRoutedOrdersPerRun() > 0
                    && routedCount >= policy.getMaxRoutedOrdersPerRun()) {
                return "HEDGE_EXECUTION_POLICY_MAX_ORDERS";
            }
            BigDecimal maxNotional = policy.getMaxRoutedNotionalPerRun();
            if (maxNotional != null && maxNotional.signum() > 0
                    && routedNotional.add(orderNotional(decision)).compareTo(maxNotional) > 0) {
                return "HEDGE_EXECUTION_POLICY_MAX_NOTIONAL";
            }
            return null;
        }

        private void record(HedgeStrategyDecision decision) {
            routedCount++;
            routedNotional = routedNotional.add(orderNotional(decision));
        }
    }

    private static BigDecimal orderNotional(HedgeStrategyDecision decision) {
        if (decision == null || decision.orderRequest() == null) {
            return BigDecimal.ZERO;
        }
        return decision.orderRequest().quantity().multiply(decision.orderRequest().referencePrice());
    }
}
