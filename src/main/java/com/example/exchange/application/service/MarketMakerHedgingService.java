/*
 * 檔案用途：應用服務，執行做市商對沖決策、風控檢查與 venue routing。
 */
package com.example.exchange.application.service;

import com.example.exchange.application.event.DomainEventPublisher;
import com.example.exchange.domain.event.HedgeDecisionRecorded;
import com.example.exchange.domain.model.dto.HedgeDecision;
import com.example.exchange.domain.model.dto.HedgeDecisionAuditRecord;
import com.example.exchange.domain.model.dto.HedgeOrderRequest;
import com.example.exchange.domain.model.dto.HedgeOrderResult;
import com.example.exchange.domain.model.dto.MarketMakerProfile;
import com.example.exchange.domain.model.dto.MarketMakerRiskLimit;
import com.example.exchange.domain.repository.HedgeDecisionAuditStore;
import com.example.exchange.domain.service.HedgeVenueAdapter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class MarketMakerHedgingService {

    private final HedgeVenueAdapter hedgeVenueAdapter;
    private final DomainEventPublisher<HedgeDecisionRecorded> publisher;
    private HedgeDecisionAuditStore auditStore;

    @Autowired(required = false)
    public void setAuditStore(HedgeDecisionAuditStore auditStore) {
        this.auditStore = auditStore;
    }

    public HedgeDecision hedge(MarketMakerProfile profile, HedgeOrderRequest request) {
        Instant now = Instant.now();
        BigDecimal orderNotional = request.quantity().abs().multiply(request.limitPrice());
        MarketMakerRiskLimit limit = profile.riskLimit(request.symbol())
                .orElse(null);

        String rejection = rejectionReason(profile, request, limit, orderNotional);
        if (rejection != null) {
            HedgeDecision decision = new HedgeDecision(
                    profile.marketMakerId(),
                    request.symbol(),
                    false,
                    rejection,
                    orderNotional,
                    null,
                    now
            );
            auditAndPublish(decision, request.refId());
            return decision;
        }

        HedgeOrderResult result = hedgeVenueAdapter.submit(request);
        HedgeDecision decision = new HedgeDecision(
                profile.marketMakerId(),
                request.symbol(),
                result.accepted(),
                result.accepted() ? null : result.reason(),
                orderNotional,
                result,
                now
        );
        auditAndPublish(decision, request.refId());
        return decision;
    }

    private static String rejectionReason(
            MarketMakerProfile profile,
            HedgeOrderRequest request,
            MarketMakerRiskLimit limit,
            BigDecimal orderNotional
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
        if (limit.maxOrderNotional().signum() > 0 && orderNotional.compareTo(limit.maxOrderNotional()) > 0) {
            return "MAX_ORDER_NOTIONAL_EXCEEDED";
        }
        BigDecimal slippageRate = slippageRate(request);
        if (slippageRate.compareTo(limit.maxSlippageRate()) > 0) {
            return "MAX_SLIPPAGE_EXCEEDED";
        }
        return null;
    }

    private static BigDecimal slippageRate(HedgeOrderRequest request) {
        if (request.referencePrice().signum() <= 0) {
            return BigDecimal.ONE;
        }
        return request.limitPrice()
                .subtract(request.referencePrice())
                .abs()
                .divide(request.referencePrice(), 18, RoundingMode.HALF_UP);
    }

    private void auditAndPublish(HedgeDecision decision, String refId) {
        if (auditStore != null) {
            auditStore.append(new HedgeDecisionAuditRecord(
                    null,
                    decision.marketMakerId(),
                    decision.symbol(),
                    decision.accepted(),
                    decision.reason(),
                    decision.orderNotional(),
                    decision.orderResult() == null ? null : decision.orderResult().venueOrderId(),
                    refId,
                    refId,
                    decision.decidedAt(),
                    null
            ));
        }
        publisher.publish(new HedgeDecisionRecorded(
                decision.marketMakerId(),
                decision.symbol(),
                decision.accepted(),
                decision.reason(),
                decision.orderNotional(),
                decision.orderResult() == null ? null : decision.orderResult().venueOrderId(),
                refId,
                decision.decidedAt()
        ));
    }
}
