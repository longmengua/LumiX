/*
 * 檔案用途：應用服務，掃描 open positions 並觸發 liquidation 判斷。
 */
package com.example.exchange.application.service;

import com.example.exchange.domain.model.dto.LiquidationResult;
import com.example.exchange.domain.model.dto.LiquidationScanResult;
import com.example.exchange.domain.model.entity.Position;
import com.example.exchange.domain.repository.PositionRepository;
import com.example.exchange.infra.config.RiskControlsProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Liquidation scanner。
 *
 * <p>Production 可由 scheduler 或 worker 呼叫本服務。服務本身只負責掃 open positions
 * 並委派 `LiquidationService`，實際 halt/manual-review/audit 邏輯仍集中在 liquidation path。</p>
 */
@Service
@RequiredArgsConstructor
public class LiquidationScanService {

    private final PositionRepository positionRepository;
    private final LiquidationService liquidationService;
    private RiskControlsProperties riskControlsProperties;

    @Autowired(required = false)
    public void setRiskControlsProperties(RiskControlsProperties riskControlsProperties) {
        this.riskControlsProperties = riskControlsProperties;
    }

    /**
     * 掃描全部 open positions，逐一使用 oracle mark price 執行 liquidation 判斷。
     */
    public LiquidationScanResult scanOpenPositions() {
        List<Position> positions = limitedPositions(positionRepository.findOpenPositions());
        List<LiquidationResult> results = new ArrayList<>();
        int liquidated = 0;
        int reviewed = 0;
        for (Position position : positions) {
            if (position == null || position.getSymbol() == null) continue;
            try {
                LiquidationResult result = liquidationService.liquidate(position.getUid(), position.getSymbol().code());
                results.add(result);
                if (result.liquidated()) {
                    liquidated++;
                } else {
                    reviewed++;
                }
            } catch (RuntimeException e) {
                // 單一倉位的 oracle/config/operation failure 不應中止整批 scanner。
                reviewed++;
            }
        }
        return new LiquidationScanResult(
                positions.size(),
                liquidated,
                reviewed,
                List.copyOf(results),
                Instant.now()
        );
    }

    private List<Position> limitedPositions(List<Position> positions) {
        if (positions == null || positions.isEmpty()) {
            return List.of();
        }
        int batchSize =
                riskControlsProperties == null
                        ? 0
                        : riskControlsProperties.getLiquidationScanBatchSize();
        if (batchSize <= 0 || positions.size() <= batchSize) {
            return positions;
        }
        return positions.subList(0, batchSize);
    }
}
