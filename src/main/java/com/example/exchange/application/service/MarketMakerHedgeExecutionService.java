/*
 * 檔案用途：應用服務，串接 exposure、reduce-only strategy 與 hedging venue routing。
 */
package com.example.exchange.application.service;

import com.example.exchange.domain.model.dto.HedgeDecision;
import com.example.exchange.domain.model.dto.HedgeExecutionReport;
import com.example.exchange.domain.model.dto.HedgeStrategyDecision;
import com.example.exchange.domain.model.dto.MarketMakerExposure;
import com.example.exchange.domain.model.dto.MarketMakerProfile;
import com.example.exchange.infra.config.RiskControlsProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MarketMakerHedgeExecutionService {

    private final MarketMakerProfileService profileService;
    private final MarketMakerExposureService exposureService;
    private final MarketMakerHedgeStrategyService strategyService;
    private final MarketMakerHedgingService hedgingService;
    private final RiskControlsProperties riskControlsProperties;
    private CommandTransactionBoundary commandTransactionBoundary;

    @Autowired(required = false)
    public void setCommandTransactionBoundary(CommandTransactionBoundary commandTransactionBoundary) {
        this.commandTransactionBoundary = commandTransactionBoundary;
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
        return profileService.enabledProfiles().stream()
                .map(profile -> execute(profile, refPrefix))
                .toList();
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
            strategyDecisions.add(strategyDecision);
            if (strategyDecision.hedgeRequired()) {
                hedgeDecisions.add(hedgingService.hedge(profile, strategyDecision.orderRequest()));
            }
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
