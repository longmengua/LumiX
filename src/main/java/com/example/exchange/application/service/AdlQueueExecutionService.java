/*
 * 檔案用途：應用服務，將 ADL queue entry 串接 ranking、planning 與 forced execution。
 */
package com.example.exchange.application.service;

import com.example.exchange.application.command.ExecuteAdlCommand;
import com.example.exchange.application.usecase.ExecuteAdlUseCase;
import com.example.exchange.domain.model.dto.AdlDeleveragingPlan;
import com.example.exchange.domain.model.dto.AdlExecutionResult;
import com.example.exchange.domain.model.dto.AdlQueueEntry;
import com.example.exchange.domain.model.dto.AdlRankingCandidate;
import com.example.exchange.domain.model.entity.Position;
import com.example.exchange.domain.repository.PositionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdlQueueExecutionService {

    private final InsuranceFundService insuranceFundService;
    private final PositionRepository positionRepository;
    private final MarkPriceOracleService markPriceOracleService;
    private final AdlRankingService adlRankingService;
    private final AdlDeleveragingPlanner adlDeleveragingPlanner;
    private final ExecuteAdlUseCase executeAdlUseCase;

    public AdlExecutionResult execute(String commandId, String liquidationId) {
        return execute(commandId, liquidationId, null);
    }

    public AdlExecutionResult execute(String commandId, String liquidationId, String operatorId) {
        AdlQueueEntry entry = findEntry(liquidationId);
        requireOwnerAllowed(entry, operatorId);
        BigDecimal markPrice = markPriceOracleService.requireMarkPrice(entry.symbol());
        List<AdlRankingCandidate> candidates = positionRepository.findOpenPositions().stream()
                .filter(position -> isCandidate(position, entry))
                .map(position -> candidate(position, markPrice))
                .toList();
        AdlDeleveragingPlan plan = adlDeleveragingPlanner.plan(
                entry.amount(),
                adlRankingService.rank(candidates),
                Map.of(entry.symbol(), markPrice)
        );
        AdlExecutionResult result = executeAdlUseCase.handle(new ExecuteAdlCommand(commandId, plan));
        if (result.executed()) {
            insuranceFundService.updateAdlRemaining(entry.liquidationId(), result.remainingNotional());
        }
        return result;
    }

    private AdlQueueEntry findEntry(String liquidationId) {
        if (liquidationId == null || liquidationId.isBlank()) {
            throw new IllegalArgumentException("liquidationId must not be blank");
        }
        return insuranceFundService.adlQueue().stream()
                .filter(entry -> liquidationId.trim().equals(entry.liquidationId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("ADL queue entry not found: " + liquidationId));
    }

    private static boolean isCandidate(Position position, AdlQueueEntry entry) {
        if (position == null || position.getQty() == null || position.getQty().signum() == 0) return false;
        if (position.getUid() == entry.uid()) return false;
        if (position.getSymbol() == null || !entry.symbol().equals(position.getSymbol().code())) return false;
        if ("LONG".equals(entry.liquidatedSide())) {
            return position.getQty().signum() < 0;
        }
        if ("SHORT".equals(entry.liquidatedSide())) {
            return position.getQty().signum() > 0;
        }
        return true;
    }

    private static AdlRankingCandidate candidate(Position position, BigDecimal markPrice) {
        return new AdlRankingCandidate(
                position.getUid(),
                position.getSymbol().code(),
                position.getQty(),
                position.getEntryPrice(),
                markPrice,
                position.getMargin(),
                position.getLeverage()
        );
    }

    private static void requireOwnerAllowed(AdlQueueEntry entry, String operatorId) {
        if (!"CLAIMED".equals(entry.status())) return;
        String normalizedOperator = operatorId == null ? "" : operatorId.trim();
        if (!entry.owner().equals(normalizedOperator)) {
            throw new IllegalStateException("ADL queue entry is claimed by " + entry.owner());
        }
    }
}
