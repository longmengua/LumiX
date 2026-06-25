/*
 * 檔案用途：應用服務，執行 ADL forced deleveraging plan 並寫入 position、ledger 與 audit event。
 */
package com.example.exchange.application.service;

import com.example.exchange.application.event.DomainEventPublisher;
import com.example.exchange.domain.event.AdlForcedDeleveragingRecorded;
import com.example.exchange.domain.model.dto.AdlDeleveragingPlan;
import com.example.exchange.domain.model.dto.AdlDeleveragingStep;
import com.example.exchange.domain.model.dto.AdlExecutionResult;
import com.example.exchange.domain.model.dto.AdlExecutionStepResult;
import com.example.exchange.domain.model.dto.PositionChange;
import com.example.exchange.domain.model.dto.Account;
import com.example.exchange.domain.model.dto.Position;
import com.example.exchange.domain.model.dto.Symbol;
import com.example.exchange.domain.model.dto.SymbolConfig;
import com.example.exchange.domain.repository.AccountRepository;
import com.example.exchange.domain.repository.AdlExecutionStore;
import com.example.exchange.domain.repository.PositionRepository;
import com.example.exchange.domain.repository.SymbolConfigRepository;
import com.example.exchange.infra.config.RiskControlsProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class AdlForcedExecutionService {

    private static final int MONEY_SCALE = 18;

    private final AccountRepository accountRepository;
    private final PositionRepository positionRepository;
    private final SymbolConfigRepository symbolConfigRepository;
    private final WalletLedgerService walletLedgerService;
    private final DomainEventPublisher<Object> publisher;
    private final Map<String, AdlExecutionResult> completedCommands = new ConcurrentHashMap<>();
    private AdlExecutionStore adlExecutionStore;
    private RiskControlsProperties riskControlsProperties;

    @Autowired(required = false)
    public void setAdlExecutionStore(AdlExecutionStore adlExecutionStore) {
        this.adlExecutionStore = adlExecutionStore;
    }

    @Autowired(required = false)
    public void setRiskControlsProperties(RiskControlsProperties riskControlsProperties) {
        this.riskControlsProperties = riskControlsProperties;
    }

    public AdlExecutionResult execute(String commandId, AdlDeleveragingPlan plan) {
        String normalizedCommandId = requireCommandId(commandId);
        AdlExecutionResult previous = findCompleted(normalizedCommandId);
        if (previous != null) return previous;
        Instant now = Instant.now();
        validatePlan(plan);

        if (riskControlsProperties != null && riskControlsProperties.isLiquidationHalt()) {
            AdlExecutionResult halted = result(normalizedCommandId, false, "ADL_HALTED", plan, BigDecimal.ZERO, BigDecimal.ZERO, List.of(), now);
            rememberRejected(halted);
            publish(halted);
            throw new IllegalStateException("ADL execution is halted");
        }
        if (riskControlsProperties != null && riskControlsProperties.isLiquidationManualReview()) {
            AdlExecutionResult result = result(normalizedCommandId, false, "ADL_MANUAL_REVIEW", plan, BigDecimal.ZERO, BigDecimal.ZERO, List.of(), now);
            rememberRejected(result);
            publish(result);
            return result;
        }

        List<PreparedStep> prepared = plan.steps().stream()
                .map(this::prepare)
                .toList();
        if (!tryStart(normalizedCommandId, plan, now)) {
            AdlExecutionResult completed = findCompleted(normalizedCommandId);
            if (completed != null) return completed;
            throw new IllegalStateException("ADL command is already in progress: " + normalizedCommandId);
        }

        List<AdlExecutionStepResult> stepResults = new ArrayList<>();
        BigDecimal executedNotional = BigDecimal.ZERO;
        BigDecimal socializedLossCharged = BigDecimal.ZERO;
        for (PreparedStep step : prepared) {
            AdlExecutionStepResult stepResult = applyStep(normalizedCommandId, step);
            stepResults.add(stepResult);
            executedNotional = executedNotional.add(step.step.reduceNotional());
            socializedLossCharged = socializedLossCharged.add(stepResult.socializedLossCharged());
        }

        AdlExecutionResult result = result(
                normalizedCommandId,
                !stepResults.isEmpty(),
                "EXECUTED",
                plan,
                executedNotional,
                socializedLossCharged,
                List.copyOf(stepResults),
                now
        );
        rememberCompleted(result);
        publish(result);
        return result;
    }

    public List<AdlExecutionResult> recentExecutions(int limit) {
        int safeLimit = validateReportLimit(limit);
        if (adlExecutionStore != null) {
            return adlExecutionStore.findRecent(safeLimit);
        }
        return completedCommands.values().stream()
                .sorted(Comparator.comparing(AdlExecutionResult::executedAt, Comparator.nullsFirst(Comparator.naturalOrder())).reversed())
                .limit(safeLimit)
                .toList();
    }

    private PreparedStep prepare(AdlDeleveragingStep step) {
        if (step.reduceQty() == null || step.reduceQty().signum() <= 0) {
            throw new IllegalArgumentException("ADL reduce qty must be positive");
        }
        if (step.reduceNotional() == null || step.reduceNotional().signum() <= 0) {
            throw new IllegalArgumentException("ADL reduce notional must be positive");
        }
        SymbolConfig config = symbolConfigRepository.findBySymbol(step.symbol())
                .orElseThrow(() -> new IllegalArgumentException("missing symbol config: " + step.symbol()));
        Symbol symbol = config.toSymbol();
        Position position = positionRepository.find(step.uid(), symbol)
                .orElseThrow(() -> new IllegalStateException("ADL candidate position not found: " + step.uid() + " " + step.symbol()));
        if (position.getQty() == null || position.getQty().signum() == 0) {
            throw new IllegalStateException("ADL candidate has no open position: " + step.uid() + " " + step.symbol());
        }
        if (position.getQty().abs().compareTo(step.reduceQty()) < 0) {
            throw new IllegalStateException("ADL candidate quantity is insufficient: " + step.uid() + " " + step.symbol());
        }
        BigDecimal executionPrice = step.reduceNotional().divide(step.reduceQty(), MONEY_SCALE, RoundingMode.HALF_UP);
        return new PreparedStep(step, config, position, executionPrice);
    }

    private AdlExecutionStepResult applyStep(String commandId, PreparedStep prepared) {
        Position position = prepared.position;
        AdlDeleveragingStep step = prepared.step;
        String refId = commandId + ":" + step.rank() + ":" + step.uid();
        BigDecimal oldQty = position.getQty();
        BigDecimal oldMargin = safe(position.getMargin());
        BigDecimal closeQty = oldQty.signum() > 0 ? step.reduceQty().negate() : step.reduceQty();
        BigDecimal closeRatio = step.reduceQty().divide(oldQty.abs(), MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal marginToRelease = oldMargin.multiply(closeRatio);

        Account account = accountRepository.findByUid(step.uid()).orElseGet(() -> new Account(step.uid()));
        BigDecimal releasableMargin = marginToRelease.min(account.crossPositionMargin());
        if (releasableMargin.signum() > 0) {
            walletLedgerService.releasePositionMargin(step.uid(), prepared.config.getQuoteAsset(), releasableMargin, refId);
        }

        PositionChange change = position.applyTradeWithPnl(closeQty, prepared.executionPrice);
        walletLedgerService.applyRealizedPnl(step.uid(), prepared.config.getQuoteAsset(), change.realizedPnl(), refId);
        BigDecimal socializedLoss = change.realizedPnl().signum() > 0
                ? change.realizedPnl().min(step.reduceNotional())
                : BigDecimal.ZERO;
        walletLedgerService.applyAdlForcedLoss(step.uid(), prepared.config.getQuoteAsset(), socializedLoss, refId);

        BigDecimal remainingMargin = oldMargin.subtract(marginToRelease);
        position.setMargin(position.getQty().signum() == 0 || remainingMargin.signum() < 0
                ? BigDecimal.ZERO
                : remainingMargin);
        positionRepository.save(position);

        return new AdlExecutionStepResult(
                step.rank(),
                step.uid(),
                step.symbol(),
                prepared.executionPrice,
                closeQty,
                change.realizedPnl(),
                releasableMargin,
                socializedLoss
        );
    }

    private void validatePlan(AdlDeleveragingPlan plan) {
        if (plan == null) {
            throw new IllegalArgumentException("ADL plan must not be null");
        }
        if (plan.steps() == null || plan.steps().isEmpty()) {
            throw new IllegalArgumentException("ADL plan must contain at least one step");
        }
    }

    private void publish(AdlExecutionResult result) {
        publisher.publish(new AdlForcedDeleveragingRecorded(
                result.commandId(),
                result.executed(),
                result.reason(),
                result.requestedNotional(),
                result.plannedNotional(),
                result.executedNotional(),
                result.socializedLossCharged(),
                result.executedAt()
        ));
    }

    private AdlExecutionResult findCompleted(String commandId) {
        if (adlExecutionStore != null) {
            return adlExecutionStore.findCompleted(commandId).orElse(null);
        }
        return completedCommands.get(commandId);
    }

    private boolean tryStart(String commandId, AdlDeleveragingPlan plan, Instant startedAt) {
        if (adlExecutionStore != null) {
            return adlExecutionStore.tryStart(commandId, plan, startedAt);
        }
        return !completedCommands.containsKey(commandId);
    }

    private void rememberCompleted(AdlExecutionResult result) {
        completedCommands.put(result.commandId(), result);
        if (adlExecutionStore != null) {
            adlExecutionStore.complete(result);
        }
    }

    private void rememberRejected(AdlExecutionResult result) {
        completedCommands.put(result.commandId(), result);
        if (adlExecutionStore != null) {
            adlExecutionStore.reject(result);
        }
    }

    private static AdlExecutionResult result(
            String commandId,
            boolean executed,
            String reason,
            AdlDeleveragingPlan plan,
            BigDecimal executedNotional,
            BigDecimal socializedLossCharged,
            List<AdlExecutionStepResult> steps,
            Instant executedAt
    ) {
        return new AdlExecutionResult(
                commandId,
                executed,
                reason,
                safe(plan.requestedNotional()),
                safe(plan.plannedNotional()),
                safe(executedNotional),
                safe(plan.remainingNotional()),
                safe(socializedLossCharged),
                steps,
                executedAt
        );
    }

    private static String requireCommandId(String commandId) {
        if (commandId == null || commandId.isBlank()) {
            throw new IllegalArgumentException("ADL command id must not be blank");
        }
        return commandId.trim();
    }

    private static int validateReportLimit(int limit) {
        if (limit <= 0 || limit > 500) {
            throw new IllegalArgumentException("ADL execution report limit must be between 1 and 500");
        }
        return limit;
    }

    private static BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private record PreparedStep(
            AdlDeleveragingStep step,
            SymbolConfig config,
            Position position,
            BigDecimal executionPrice
    ) {
    }
}
