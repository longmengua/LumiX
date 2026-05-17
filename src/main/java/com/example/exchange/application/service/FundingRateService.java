package com.example.exchange.application.service;

import com.example.exchange.application.event.DomainEventPublisher;
import com.example.exchange.domain.event.FundingSettled;
import com.example.exchange.domain.model.dto.FundingSettlementResult;
import com.example.exchange.domain.model.entity.Position;
import com.example.exchange.domain.model.entity.SymbolConfig;
import com.example.exchange.domain.repository.PositionRepository;
import com.example.exchange.domain.repository.SymbolConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FundingRateService {

    private final PositionRepository positionRepository;
    private final SymbolConfigRepository symbolConfigRepository;
    private final WalletLedgerService walletLedgerService;
    private final DomainEventPublisher<FundingSettled> publisher;

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
        return List.of();
    }
}
