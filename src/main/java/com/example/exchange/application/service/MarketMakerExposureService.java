/*
 * 檔案用途：應用服務，彙總做市商持倉 exposure。
 */
package com.example.exchange.application.service;

import com.example.exchange.domain.model.dto.MarketMakerExposure;
import com.example.exchange.domain.model.dto.MarketMakerProfile;
import com.example.exchange.domain.model.entity.Position;
import com.example.exchange.domain.repository.PositionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MarketMakerExposureService {

    private final PositionRepository positionRepository;
    private final MarkPriceOracleService markPriceOracleService;

    public List<MarketMakerExposure> exposures(MarketMakerProfile profile) {
        return positionRepository.findAllByUid(profile.uid()).stream()
                .filter(position -> position.getSymbol() != null)
                .filter(position -> position.getQty() != null && position.getQty().signum() != 0)
                .map(position -> exposure(profile, position))
                .toList();
    }

    private MarketMakerExposure exposure(MarketMakerProfile profile, Position position) {
        String symbol = position.getSymbol().code();
        BigDecimal markPrice = markPriceOracleService.requireMarkPrice(symbol);
        BigDecimal notional = position.getQty().multiply(markPrice);
        return new MarketMakerExposure(
                profile.marketMakerId(),
                profile.uid(),
                symbol,
                position.getQty(),
                markPrice,
                notional
        );
    }
}
