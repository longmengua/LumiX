/*
 * 檔案用途：應用服務，編排領域模型、Repository 與外部基礎設施完成業務流程。
 */
package com.example.exchange.application.service;

import com.example.exchange.application.event.DomainEventPublisher;
import com.example.exchange.domain.event.FundingSettled;
import com.example.exchange.domain.model.dto.FundingSettlementResult;
import com.example.exchange.domain.model.dto.Position;
import com.example.exchange.domain.model.dto.SymbolConfig;
import com.example.exchange.domain.repository.PositionRepository;
import com.example.exchange.domain.repository.SymbolConfigRepository;
import com.example.exchange.infra.config.FundingRateProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FundingRateService {

    private final PositionRepository positionRepository;
    private final SymbolConfigRepository symbolConfigRepository;
    private final WalletLedgerService walletLedgerService;
    private final DomainEventPublisher<FundingSettled> publisher;
    private final FundingRateProperties fundingRateProperties;
    private MarkPriceOracleService markPriceOracleService;

    @Autowired(required = false)
    public void setMarkPriceOracleService(MarkPriceOracleService markPriceOracleService) {
        this.markPriceOracleService = markPriceOracleService;
    }

    public FundingSettlementResult settle(
            long uid,
            String symbol,
            BigDecimal fundingRate
    ) {
        return settle(uid, symbol, requireOracleMarkPrice(symbol), fundingRate);
    }

    public FundingSettlementResult settle(
            long uid,
            String symbol,
            BigDecimal markPrice,
            BigDecimal fundingRate
    ) {
        SymbolConfig config = symbolConfigRepository.findBySymbol(symbol)
                .orElseThrow(() -> new IllegalArgumentException("missing symbol config: " + symbol));
        Position position = positionRepository.find(uid, config.toSymbol()).orElse(null);
        Instant now = Instant.now();
        String settlementId = "funding-" + UUID.randomUUID();

        if (position == null || position.getQty() == null || position.getQty().signum() == 0) {
            return new FundingSettlementResult(
                    uid,
                    config.getSymbol(),
                    markPrice,
                    fundingRate,
                    BigDecimal.ZERO,
                    settlementId,
                    false,
                    now
            );
        }

        BigDecimal cashflow = position.getQty()
                .negate()
                .multiply(markPrice)
                .multiply(fundingRate);
        walletLedgerService.applyFundingFee(uid, config.getQuoteAsset(), cashflow, settlementId);
        position.addFunding(cashflow);
        positionRepository.save(position);

        FundingSettled event = new FundingSettled(
                uid,
                config.toSymbol(),
                markPrice,
                fundingRate,
                cashflow,
                settlementId,
                now
        );
        publisher.publish(event);

        return new FundingSettlementResult(
                uid,
                config.getSymbol(),
                markPrice,
                fundingRate,
                cashflow,
                settlementId,
                true,
                now
        );
    }

    public List<FundingSettlementResult> settleConfiguredSymbols() {
        Map<String, FundingRateProperties.Settlement> settlements = configuredSettlementsBySymbol();
        if (settlements.isEmpty()) return List.of();

        return positionRepository.findOpenPositions().stream()
                .filter(position -> position.getSymbol() != null)
                .map(position -> {
                    FundingRateProperties.Settlement settlement =
                            settlements.get(normalize(position.getSymbol().code()));
                    if (!isValidSettlement(settlement)) return null;
                    return settle(
                            position.getUid(),
                            position.getSymbol().code(),
                            settlement.getFundingRate()
                    );
                })
                .filter(result -> result != null)
                .toList();
    }

    private Map<String, FundingRateProperties.Settlement> configuredSettlementsBySymbol() {
        Map<String, FundingRateProperties.Settlement> settlements = new LinkedHashMap<>();
        for (FundingRateProperties.Settlement settlement : fundingRateProperties.getSettlements()) {
            String symbol = settlement == null ? "" : normalize(settlement.getSymbol());
            if (!symbol.isBlank()) {
                settlements.put(symbol, settlement);
            }
        }
        return settlements;
    }

    private static boolean isValidSettlement(FundingRateProperties.Settlement settlement) {
        return settlement != null
                && settlement.getFundingRate() != null;
    }

    private BigDecimal requireOracleMarkPrice(String symbol) {
        if (markPriceOracleService == null) {
            throw new IllegalStateException("mark price oracle is not configured");
        }
        return markPriceOracleService.requireMarkPrice(symbol);
    }

    private static String normalize(String symbol) {
        return symbol == null ? "" : symbol.trim().toUpperCase();
    }
}
