/*
 * 檔案用途：應用服務，處理做市商 quote command 的基礎風控檢查。
 */
package com.example.exchange.application.service;

import com.example.exchange.application.event.DomainEventPublisher;
import com.example.exchange.domain.event.MarketMakerQuoteDecisionRecorded;
import com.example.exchange.domain.model.dto.MarketMakerProfile;
import com.example.exchange.domain.model.dto.MarketMakerQuoteCommand;
import com.example.exchange.domain.model.dto.MarketMakerQuoteDecision;
import com.example.exchange.domain.model.dto.MarketMakerRiskLimit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class MarketMakerQuoteService {

    private final DomainEventPublisher<MarketMakerQuoteDecisionRecorded> publisher;

    public MarketMakerQuoteDecision validateQuote(MarketMakerProfile profile, MarketMakerQuoteCommand command) {
        String reason = rejectionReason(profile, command);
        MarketMakerQuoteDecision decision = new MarketMakerQuoteDecision(
                profile.marketMakerId(),
                command.symbol(),
                reason == null,
                reason,
                Instant.now()
        );
        publisher.publish(new MarketMakerQuoteDecisionRecorded(
                decision.marketMakerId(),
                decision.symbol(),
                decision.accepted(),
                decision.reason(),
                command.refId(),
                decision.decidedAt()
        ));
        return decision;
    }

    private static String rejectionReason(MarketMakerProfile profile, MarketMakerQuoteCommand command) {
        if (!profile.enabled()) {
            return "MARKET_MAKER_DISABLED";
        }
        MarketMakerRiskLimit limit = profile.riskLimit(command.symbol()).orElse(null);
        if (limit == null) {
            return "RISK_LIMIT_NOT_CONFIGURED";
        }
        if (limit.killSwitch()) {
            return "KILL_SWITCH_ENABLED";
        }
        if (notPositive(command.bidPrice()) || notPositive(command.bidQuantity())
                || notPositive(command.askPrice()) || notPositive(command.askQuantity())) {
            return "INVALID_QUOTE";
        }
        if (command.bidPrice().compareTo(command.askPrice()) >= 0) {
            return "CROSSED_QUOTE";
        }
        return null;
    }

    private static boolean notPositive(BigDecimal value) {
        return value == null || value.signum() <= 0;
    }
}
