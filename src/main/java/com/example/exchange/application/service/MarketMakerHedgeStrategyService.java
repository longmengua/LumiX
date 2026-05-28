/*
 * 檔案用途：應用服務，根據做市商 inventory exposure 產生 reduce-only hedge 建議。
 */
package com.example.exchange.application.service;

import com.example.exchange.domain.model.dto.HedgeOrderRequest;
import com.example.exchange.domain.model.dto.HedgeStrategyDecision;
import com.example.exchange.domain.model.dto.MarketMakerExposure;
import com.example.exchange.domain.model.dto.MarketMakerProfile;
import com.example.exchange.domain.model.dto.MarketMakerRiskLimit;
import com.example.exchange.domain.model.enums.OrderSide;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class MarketMakerHedgeStrategyService {

    public HedgeStrategyDecision planReduceOnlyHedge(
            MarketMakerProfile profile,
            MarketMakerExposure exposure,
            String refId
    ) {
        MarketMakerRiskLimit limit = profile.riskLimit(exposure.symbol()).orElse(null);
        String rejection = planningRejection(profile, exposure, limit);
        if (rejection != null) {
            return decision(profile, exposure, false, rejection, null);
        }
        BigDecimal targetNotional = targetHedgeNotional(exposure, limit);
        if (targetNotional.signum() <= 0) {
            return decision(profile, exposure, false, "WITHIN_INVENTORY_LIMIT", null);
        }
        BigDecimal cappedNotional = capOrderNotional(targetNotional, limit.maxOrderNotional());
        HedgeOrderRequest request = request(profile, exposure, limit, cappedNotional, refId);
        return decision(profile, exposure, true, "REDUCE_ONLY_HEDGE_REQUIRED", request);
    }

    private static String planningRejection(
            MarketMakerProfile profile,
            MarketMakerExposure exposure,
            MarketMakerRiskLimit limit
    ) {
        if (!profile.enabled()) {
            return "MARKET_MAKER_DISABLED";
        }
        if (limit == null) {
            return "RISK_LIMIT_NOT_CONFIGURED";
        }
        if (limit.killSwitch()) {
            return "KILL_SWITCH_ENABLED";
        }
        if (exposure.markPrice().signum() <= 0 || exposure.quantity().signum() == 0) {
            return "NO_HEDGE_REQUIRED";
        }
        return null;
    }

    private static BigDecimal targetHedgeNotional(MarketMakerExposure exposure, MarketMakerRiskLimit limit) {
        BigDecimal notional = exposure.notional();
        if (notional.signum() > 0) {
            return excessNotional(notional, limit.maxLongNotional());
        }
        return excessNotional(notional.abs(), limit.maxShortNotional());
    }

    private static BigDecimal excessNotional(BigDecimal absoluteNotional, BigDecimal allowedNotional) {
        if (allowedNotional.signum() <= 0) {
            return absoluteNotional;
        }
        return absoluteNotional.subtract(allowedNotional).max(BigDecimal.ZERO);
    }

    private static BigDecimal capOrderNotional(BigDecimal targetNotional, BigDecimal maxOrderNotional) {
        if (maxOrderNotional.signum() > 0 && targetNotional.compareTo(maxOrderNotional) > 0) {
            return maxOrderNotional;
        }
        return targetNotional;
    }

    private static HedgeOrderRequest request(
            MarketMakerProfile profile,
            MarketMakerExposure exposure,
            MarketMakerRiskLimit limit,
            BigDecimal orderNotional,
            String refId
    ) {
        OrderSide side = exposure.quantity().signum() > 0 ? OrderSide.SELL : OrderSide.BUY;
        BigDecimal referencePrice = exposure.markPrice();
        BigDecimal quantity = orderNotional.divide(referencePrice, 18, RoundingMode.HALF_UP);
        BigDecimal limitPrice = limitPrice(side, referencePrice, limit.maxSlippageRate());
        return new HedgeOrderRequest(
                profile.marketMakerId(),
                profile.uid(),
                exposure.symbol(),
                side,
                quantity,
                referencePrice,
                limitPrice,
                refId
        );
    }

    private static BigDecimal limitPrice(OrderSide side, BigDecimal referencePrice, BigDecimal slippageRate) {
        BigDecimal multiplier = OrderSide.BUY.equals(side)
                ? BigDecimal.ONE.add(slippageRate)
                : BigDecimal.ONE.subtract(slippageRate);
        return referencePrice.multiply(multiplier);
    }

    private static HedgeStrategyDecision decision(
            MarketMakerProfile profile,
            MarketMakerExposure exposure,
            boolean hedgeRequired,
            String reason,
            HedgeOrderRequest request
    ) {
        return new HedgeStrategyDecision(
                profile.marketMakerId(),
                exposure.symbol(),
                hedgeRequired,
                reason,
                exposure,
                request
        );
    }
}
