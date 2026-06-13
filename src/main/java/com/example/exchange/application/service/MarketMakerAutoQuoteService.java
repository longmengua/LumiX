/*
 * 檔案用途：應用服務，依據目前 order book top-of-book 自動產生做市商雙邊 quote。
 */
package com.example.exchange.application.service;

import com.example.exchange.domain.model.dto.MarketMakerAutoQuoteResult;
import com.example.exchange.domain.model.dto.MarketMakerAutoQuoteRunReport;
import com.example.exchange.domain.model.dto.MarketMakerProfile;
import com.example.exchange.domain.model.dto.MarketMakerQuoteCommand;
import com.example.exchange.domain.model.dto.MarketMakerRiskLimit;
import com.example.exchange.domain.model.dto.MarketTicker;
import com.example.exchange.domain.model.dto.TopOfBook;
import com.example.exchange.domain.model.entity.SymbolConfig;
import com.example.exchange.domain.repository.SymbolConfigRepository;
import com.example.exchange.domain.service.MatchingEngine;
import com.example.exchange.infra.config.MarketMakerAutoQuoteProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
public class MarketMakerAutoQuoteService {

    private static final BigDecimal TWO = new BigDecimal("2");

    private final MarketMakerProfileService profileService;
    private final MarketMakerQuoteLifecycleService quoteLifecycleService;
    private final MatchingEngine matchingEngine;
    private final SymbolConfigRepository symbolConfigRepository;
    private final MarketDataService marketDataService;
    private final MarketMakerAutoQuoteProperties properties;
    private final AtomicLong sequence = new AtomicLong();

    @Transactional
    public MarketMakerAutoQuoteRunReport runOnce() {
        long runSequence = sequence.incrementAndGet();
        List<MarketMakerAutoQuoteResult> results = new ArrayList<>();
        List<MarketMakerProfile> profiles = profileService.enabledProfiles().stream()
                .limit(properties.getMaxProfilesPerRun())
                .toList();
        for (MarketMakerProfile profile : profiles) {
            for (MarketMakerRiskLimit riskLimit : profile.riskLimits()) {
                results.add(quoteSymbol(profile, riskLimit, runSequence));
            }
        }
        int placed = (int) results.stream().filter(MarketMakerAutoQuoteResult::placed).count();
        return new MarketMakerAutoQuoteRunReport(runSequence, placed, results.size() - placed, results);
    }

    private MarketMakerAutoQuoteResult quoteSymbol(
            MarketMakerProfile profile,
            MarketMakerRiskLimit riskLimit,
            long runSequence
    ) {
        if (!profile.enabled()) {
            return skipped(profile.marketMakerId(), riskLimit.symbol(), "PROFILE_DISABLED");
        }
        if (riskLimit.killSwitch()) {
            return skipped(profile.marketMakerId(), riskLimit.symbol(), "KILL_SWITCH_ENABLED");
        }
        String symbol = normalizeSymbol(riskLimit.symbol());
        Optional<SymbolConfig> maybeConfig = symbolConfigRepository.findBySymbol(symbol);
        if (maybeConfig.isEmpty() || !maybeConfig.get().isTradingEnabled()) {
            return skipped(profile.marketMakerId(), symbol, "SYMBOL_DISABLED");
        }
        Optional<TopOfBook> maybeTop = matchingEngine.top(symbol);
        if (maybeTop.isEmpty() || !hasTwoSidedTop(maybeTop.get())) {
            return skipped(profile.marketMakerId(), symbol, "NO_TWO_SIDED_TOP_OF_BOOK");
        }

        SymbolConfig config = maybeConfig.get();
        TopOfBook top = maybeTop.get();
        BigDecimal tick = positiveOrDefault(config.priceTickOrDefault(), new BigDecimal("0.01"));
        int halfSpreadTicks = Math.max(1, properties.getHalfSpreadTicks());
        BigDecimal pulse = quotePulse(runSequence, tick);
        BigDecimal mid = safeFairMid(symbol, top);
        BigDecimal halfSpread = safeHalfSpread(config, mid, tick, halfSpreadTicks);
        BigDecimal nearestBid = floorToStep(mid.subtract(halfSpread).add(pulse), tick);
        BigDecimal nearestAsk = ceilToStep(mid.add(halfSpread).add(pulse), tick);
        if (nearestBid.compareTo(nearestAsk) >= 0) {
            nearestAsk = nearestBid.add(tick);
        }

        Optional<BigDecimal> quantity = quantityFor(config, riskLimit, nearestAsk);
        if (quantity.isEmpty()) {
            return skipped(profile.marketMakerId(), symbol, "QUOTE_SIZE_BELOW_MIN_NOTIONAL_OR_RISK_LIMIT");
        }

        List<MarketMakerQuoteCommand> ladder = quoteLadder(
                profile,
                symbol,
                nearestBid,
                nearestAsk,
                tick,
                quantity.get(),
                runSequence
        );
        // 自動策略仍走 lifecycle，確保撤舊單、風控 decision、state persistence 與 WebSocket 推送一致；ladder 只撤舊單一次。
        quoteLifecycleService.placeQuoteLadder(ladder);
        String refId = ladder.getLast().refId();
        return new MarketMakerAutoQuoteResult(profile.marketMakerId(), symbol, true, "PLACED", refId);
    }

    private List<MarketMakerQuoteCommand> quoteLadder(
            MarketMakerProfile profile,
            String symbol,
            BigDecimal nearestBid,
            BigDecimal nearestAsk,
            BigDecimal tick,
            BigDecimal quantity,
            long runSequence
    ) {
        int levels = Math.max(1, properties.getLadderLevelsPerSide());
        List<MarketMakerQuoteCommand> commands = new ArrayList<>(levels);
        for (int level = 0; level < levels; level++) {
            BigDecimal offset = tick.multiply(BigDecimal.valueOf(level));
            String refId = properties.getRefPrefix() + "-" + profile.marketMakerId() + "-" + symbol + "-" + runSequence + "-" + (level + 1);
            commands.add(new MarketMakerQuoteCommand(
                    profile.marketMakerId(),
                    profile.uid(),
                    symbol,
                    nearestBid.subtract(offset).stripTrailingZeros(),
                    quantity,
                    nearestAsk.add(offset).stripTrailingZeros(),
                    quantity,
                    refId
            ));
        }
        return commands;
    }

    private BigDecimal safeFairMid(String symbol, TopOfBook internalTop) {
        Optional<MarketTicker> ticker = marketDataService.ticker(symbol);
        if (ticker.isPresent()) {
            MarketTicker fair = ticker.get();
            if (fair.bestBid() != null && fair.bestAsk() != null
                    && fair.bestBid().signum() > 0
                    && fair.bestAsk().signum() > 0
                    && fair.bestBid().compareTo(fair.bestAsk()) < 0) {
                return fair.bestBid().add(fair.bestAsk()).divide(TWO, 18, RoundingMode.HALF_UP);
            }
            if (fair.lastPrice() != null && fair.lastPrice().signum() > 0) {
                return fair.lastPrice();
            }
        }
        return internalTop.getBestBid().add(internalTop.getBestAsk()).divide(TWO, 18, RoundingMode.HALF_UP);
    }

    private BigDecimal safeHalfSpread(SymbolConfig config, BigDecimal mid, BigDecimal tick, int halfSpreadTicks) {
        BigDecimal tickSpread = tick.multiply(BigDecimal.valueOf(halfSpreadTicks));
        BigDecimal effectiveMakerFee = positiveOrZero(config.makerFeeRateOrDefault()
                .subtract(config.makerRebateRateOrDefault()));
        BigDecimal hedgeTakerFee = positiveOrZero(config.takerFeeRateOrDefault());
        BigDecimal hedgeCost = positiveOrZero(properties.getHedgeCostRate());
        BigDecimal costRate = effectiveMakerFee.add(hedgeTakerFee).add(hedgeCost);
        // Safe market making first prices around fair mid plus fee/hedge buffer; strategy tweaks happen outside it.
        BigDecimal costSpread = mid.multiply(costRate).setScale(18, RoundingMode.CEILING);
        return tickSpread.max(costSpread).max(tick);
    }

    private BigDecimal quotePulse(long runSequence, BigDecimal tick) {
        int pulseTicks = properties.getPulseTicks();
        if (pulseTicks <= 0) {
            return BigDecimal.ZERO;
        }
        long phase = Math.floorMod(runSequence, 3) - 1;
        return tick.multiply(BigDecimal.valueOf(phase * pulseTicks));
    }

    private Optional<BigDecimal> quantityFor(SymbolConfig config, MarketMakerRiskLimit riskLimit, BigDecimal askPrice) {
        BigDecimal lot = positiveOrDefault(config.lotSizeOrDefault(), new BigDecimal("0.001"));
        BigDecimal qty = ceilToStep(properties.getQuoteQuantity(), lot);
        BigDecimal minQty = positiveOrDefault(config.minQtyOrDefault(), lot);
        if (qty.compareTo(minQty) < 0) {
            qty = ceilToStep(minQty, lot);
        }
        BigDecimal minNotional = config.minNotionalOrDefault();
        if (minNotional.signum() > 0 && askPrice.multiply(qty).compareTo(minNotional) < 0) {
            qty = ceilToStep(minNotional.divide(askPrice, 18, RoundingMode.CEILING), lot);
        }

        BigDecimal maxOrderNotional = maxOrderNotional(config, riskLimit);
        if (maxOrderNotional.signum() > 0 && askPrice.multiply(qty).compareTo(maxOrderNotional) > 0) {
            qty = floorToStep(maxOrderNotional.divide(askPrice, 18, RoundingMode.FLOOR), lot);
        }
        if (qty.compareTo(minQty) < 0) {
            return Optional.empty();
        }
        if (minNotional.signum() > 0 && askPrice.multiply(qty).compareTo(minNotional) < 0) {
            return Optional.empty();
        }
        return Optional.of(qty.stripTrailingZeros());
    }

    private BigDecimal maxOrderNotional(SymbolConfig config, MarketMakerRiskLimit riskLimit) {
        BigDecimal symbolLimit = config.maxOrderNotionalOrDefault();
        BigDecimal makerLimit = riskLimit.maxOrderNotional();
        if (makerLimit == null || makerLimit.signum() <= 0) {
            return symbolLimit;
        }
        return symbolLimit.signum() <= 0 ? makerLimit : symbolLimit.min(makerLimit);
    }

    private static boolean hasTwoSidedTop(TopOfBook top) {
        return top.getBestBid() != null
                && top.getBestAsk() != null
                && top.getBestBid().signum() > 0
                && top.getBestAsk().signum() > 0
                && top.getBestBid().compareTo(top.getBestAsk()) < 0;
    }

    private static MarketMakerAutoQuoteResult skipped(String marketMakerId, String symbol, String reason) {
        return new MarketMakerAutoQuoteResult(marketMakerId, normalizeSymbol(symbol), false, reason, null);
    }

    private static String normalizeSymbol(String symbol) {
        return symbol == null ? "" : symbol.trim().toUpperCase();
    }

    private static BigDecimal positiveOrDefault(BigDecimal value, BigDecimal fallback) {
        return value == null || value.signum() <= 0 ? fallback : value;
    }

    private static BigDecimal positiveOrZero(BigDecimal value) {
        return value == null || value.signum() <= 0 ? BigDecimal.ZERO : value;
    }

    private static BigDecimal floorToStep(BigDecimal value, BigDecimal step) {
        return value.divide(step, 0, RoundingMode.FLOOR).multiply(step).stripTrailingZeros();
    }

    private static BigDecimal ceilToStep(BigDecimal value, BigDecimal step) {
        return value.divide(step, 0, RoundingMode.CEILING).multiply(step).stripTrailingZeros();
    }
}
