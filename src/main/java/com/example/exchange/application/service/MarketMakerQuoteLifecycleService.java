/*
 * 檔案用途：應用服務，將做市商 quote command 驗證後轉成實際內部掛單。
 */
package com.example.exchange.application.service;

import com.example.exchange.domain.model.dto.MarketMakerProfile;
import com.example.exchange.domain.model.dto.MarketMakerQuoteCommand;
import com.example.exchange.domain.model.dto.MarketMakerQuoteDecision;
import com.example.exchange.domain.model.dto.MarketMakerQuoteLifecycleReport;
import com.example.exchange.domain.model.enums.OrderSide;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MarketMakerQuoteLifecycleService {

    private final MarketMakerProfileService profileService;
    private final MarketMakerQuoteService quoteService;
    private final MarketMakerQuoteOrderGateway orderGateway;

    @Transactional
    public MarketMakerQuoteLifecycleReport placeQuote(MarketMakerQuoteCommand command) {
        validate(command);
        MarketMakerProfile profile = profileService.findByMarketMakerId(command.marketMakerId())
                .orElseThrow(() -> new IllegalArgumentException("market maker profile not found"));
        if (profile.uid() != command.uid()) {
            throw new IllegalArgumentException("market maker uid mismatch");
        }

        MarketMakerQuoteDecision decision = quoteService.validateQuote(profile, command);
        int canceledCount = orderGateway.cancelOpenQuoteOrders(command);
        if (!decision.accepted()) {
            return new MarketMakerQuoteLifecycleReport(decision, canceledCount, 0, null, null);
        }

        UUID bidOrderId = orderGateway.placePostOnlyLimit(command, OrderSide.BUY);
        UUID askOrderId = orderGateway.placePostOnlyLimit(command, OrderSide.SELL);
        return new MarketMakerQuoteLifecycleReport(decision, canceledCount, 2, bidOrderId, askOrderId);
    }

    private static void validate(MarketMakerQuoteCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("market maker quote command cannot be null");
        }
        if (command.marketMakerId() == null || command.marketMakerId().isBlank()) {
            throw new IllegalArgumentException("market maker id is required");
        }
        if (command.uid() <= 0) {
            throw new IllegalArgumentException("market maker uid must be positive");
        }
        if (command.symbol() == null || command.symbol().isBlank()) {
            throw new IllegalArgumentException("symbol is required");
        }
    }
}
