/*
 * 檔案用途：應用服務，將做市商 quote command 驗證後轉成實際內部掛單。
 */
package com.example.exchange.application.service;

import com.example.exchange.domain.model.dto.MarketMakerProfile;
import com.example.exchange.domain.model.dto.MarketMakerQuoteCommand;
import com.example.exchange.domain.model.dto.MarketMakerQuoteDecision;
import com.example.exchange.domain.model.dto.MarketMakerQuoteLifecycleReport;
import com.example.exchange.domain.model.dto.MarketMakerQuoteState;
import com.example.exchange.domain.model.enums.OrderSide;
import com.example.exchange.domain.repository.MarketMakerQuoteStateStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MarketMakerQuoteLifecycleService {

    private final MarketMakerProfileService profileService;
    private final MarketMakerQuoteService quoteService;
    private final MarketMakerQuoteOrderGateway orderGateway;
    private final MarketMakerQuoteStateStore quoteStateStore;

    @Transactional
    public MarketMakerQuoteLifecycleReport placeQuote(MarketMakerQuoteCommand command) {
        validate(command);
        MarketMakerProfile profile = profileService.findByMarketMakerId(command.marketMakerId())
                .orElseThrow(() -> new IllegalArgumentException("market maker profile not found"));
        if (profile.uid() != command.uid()) {
            throw new IllegalArgumentException("market maker uid mismatch");
        }

        Optional<MarketMakerQuoteState> previousState =
                quoteStateStore.find(command.marketMakerId(), command.symbol());
        MarketMakerQuoteDecision decision = quoteService.validateQuote(profile, command);
        int canceledCount = orderGateway.cancelOpenQuoteOrders(command);
        if (!decision.accepted()) {
            quoteStateStore.save(state(command, decision, canceledCount, null, null, previousState.orElse(null)));
            return new MarketMakerQuoteLifecycleReport(decision, canceledCount, 0, null, null);
        }

        UUID bidOrderId = orderGateway.placePostOnlyLimit(command, OrderSide.BUY);
        UUID askOrderId = orderGateway.placePostOnlyLimit(command, OrderSide.SELL);
        quoteStateStore.save(state(command, decision, canceledCount, bidOrderId, askOrderId, previousState.orElse(null)));
        return new MarketMakerQuoteLifecycleReport(decision, canceledCount, 2, bidOrderId, askOrderId);
    }

    @Transactional(readOnly = true)
    public Optional<MarketMakerQuoteState> quoteState(String marketMakerId, String symbol) {
        return quoteStateStore.find(marketMakerId, symbol);
    }

    @Transactional(readOnly = true)
    public List<MarketMakerQuoteState> quoteStatesByMarketMaker(String marketMakerId, int limit) {
        return quoteStateStore.findByMarketMakerId(marketMakerId, limit);
    }

    @Transactional(readOnly = true)
    public List<MarketMakerQuoteState> activeQuoteStates(int limit) {
        return quoteStateStore.findActive(limit);
    }

    private static MarketMakerQuoteState state(
            MarketMakerQuoteCommand command,
            MarketMakerQuoteDecision decision,
            int canceledCount,
            UUID bidOrderId,
            UUID askOrderId,
            MarketMakerQuoteState previousState
    ) {
        long previousBidVersion = previousState == null ? 0 : previousState.bidVersion();
        long previousAskVersion = previousState == null ? 0 : previousState.askVersion();
        return new MarketMakerQuoteState(
                command.marketMakerId().trim(),
                command.uid(),
                command.symbol().trim().toUpperCase(),
                command.refId() == null ? null : command.refId().trim(),
                decision.accepted(),
                decision.accepted(),
                decision.reason(),
                canceledCount,
                bidOrderId,
                askOrderId,
                previousBidVersion + 1,
                previousAskVersion + 1,
                previousState == null ? null : previousState.bidOrderId(),
                previousState == null ? null : previousState.askOrderId(),
                Instant.now()
        );
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
