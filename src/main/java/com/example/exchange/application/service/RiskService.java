/*
 * 檔案用途：應用服務，編排領域模型、Repository 與外部基礎設施完成業務流程。
 */
package com.example.exchange.application.service;

import com.example.exchange.domain.model.dto.TopOfBook;
import com.example.exchange.domain.model.entity.Account;
import com.example.exchange.domain.model.entity.Order;
import com.example.exchange.domain.model.entity.Position;
import com.example.exchange.domain.model.entity.SymbolConfig;
import com.example.exchange.domain.model.enums.OrderSide;
import com.example.exchange.domain.model.enums.OrderType;
import com.example.exchange.domain.repository.AccountRepository;
import com.example.exchange.domain.repository.OrderRepository;
import com.example.exchange.domain.repository.PositionRepository;
import com.example.exchange.domain.service.MatchingEngine;
import com.example.exchange.infra.config.RiskControlsProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RiskService {

    private static final int MONEY_SCALE = 18;

    private final AccountRepository accountRepo;
    private final PositionRepository positionRepo;
    private final OrderRepository orderRepo;
    private final MatchingEngine matchingEngine;
    private final WalletLedgerService walletLedgerService;
    private final RiskControlsProperties riskControlsProperties;

    public void preCheckAndReserve(Order order, SymbolConfig config) {
        validateTradable(order, config);
        validateRiskSwitches(order);
        validateMaxOpenOrders(order, config);
        validateClientOrderIdDeduplication(order, null);
        BigDecimal referencePrice = resolveReferencePrice(order);
        validateTickAndLot(order, config);
        validateNotional(order, config, referencePrice);
        validatePriceBand(order, config);
        validateLeverage(order, config);
        validateReduceOnly(order);
        validatePositionLimit(order, config, referencePrice);

        BigDecimal reserve = requiredOrderReserve(order, config, referencePrice);
        if (reserve.signum() <= 0) return;

        Account account = accountRepo.findByUid(order.getUid()).orElseGet(() -> new Account(order.getUid()));
        if (account.crossAvailable().compareTo(reserve) < 0) {
            order.reject("INSUFFICIENT_BALANCE");
            throw new IllegalStateException("insufficient available balance");
        }

        walletLedgerService.reserveOrder(
                order.getUid(),
                config.getQuoteAsset(),
                reserve,
                order.getId().toString()
        );
        order.setReservedAmount(reserve);
    }

    public BigDecimal validateAmend(Order order, SymbolConfig config) {
        validateTradable(order, config);
        validateRiskSwitches(order);
        validateClientOrderIdDeduplication(order, order.getId());
        BigDecimal referencePrice = resolveReferencePrice(order);
        validateTickAndLot(order, config);
        validateNotional(order, config, referencePrice);
        validatePriceBand(order, config);
        validateLeverage(order, config);
        validateReduceOnly(order);
        validatePositionLimit(order, config, referencePrice);
        return requiredOrderReserve(order, config, referencePrice);
    }

    public void reconcileOrderReserve(Order order, SymbolConfig config, BigDecimal targetReserve) {
        BigDecimal target = targetReserve == null ? BigDecimal.ZERO : targetReserve;
        BigDecimal current = order.getReservedAmount() == null ? BigDecimal.ZERO : order.getReservedAmount();
        BigDecimal diff = target.subtract(current);
        if (diff.signum() > 0) {
            Account account = accountRepo.findByUid(order.getUid()).orElseGet(() -> new Account(order.getUid()));
            if (account.crossAvailable().compareTo(diff) < 0) {
                order.reject("INSUFFICIENT_BALANCE");
                throw new IllegalStateException("insufficient available balance");
            }
            walletLedgerService.reserveOrder(
                    order.getUid(),
                    config.getQuoteAsset(),
                    diff,
                    order.getId().toString()
            );
        } else if (diff.signum() < 0) {
            walletLedgerService.releaseOrderReserve(
                    order.getUid(),
                    config.getQuoteAsset(),
                    diff.abs(),
                    order.getId().toString()
            );
        }
        order.setReservedAmount(target);
    }

    public BigDecimal requiredOrderReserve(Order order, SymbolConfig config, BigDecimal referencePrice) {
        if (!isOpen(order) || order.getQty() == null || order.getQty().signum() <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal px = order.getType() == OrderType.LIMIT ? order.getPrice() : referencePrice;
        if (px == null || px.signum() <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal notional = px.multiply(order.getQty());
        BigDecimal feeReserve = notional.multiply(config.takerFeeRateOrDefault());
        BigDecimal marginReserve = order.isReduceOnly()
                ? BigDecimal.ZERO
                : notional.divide(BigDecimal.valueOf(Math.max(1, order.getLeverage())), MONEY_SCALE, RoundingMode.HALF_UP);
        return marginReserve.add(feeReserve).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    public BigDecimal resolveReferencePrice(Order order) {
        if (order.getType() == OrderType.LIMIT && order.getPrice() != null) {
            return order.getPrice();
        }

        Optional<TopOfBook> top = matchingEngine.top(order.getSymbol().code());
        if (order.getSide() == OrderSide.BUY) {
            BigDecimal bestAsk = top.map(TopOfBook::getBestAsk).orElse(null);
            if (bestAsk != null && bestAsk.signum() > 0) return bestAsk;
        } else {
            BigDecimal bestBid = top.map(TopOfBook::getBestBid).orElse(null);
            if (bestBid != null && bestBid.signum() > 0) return bestBid;
        }

        order.reject("NO_LIQUIDITY");
        throw new IllegalArgumentException("market order requires opposite liquidity");
    }

    private void validateTradable(Order order, SymbolConfig config) {
        if (config == null || !config.isTradingEnabled()) {
            order.reject("SYMBOL_NOT_TRADABLE");
            throw new IllegalArgumentException("symbol is not tradable");
        }
    }

    private void validateRiskSwitches(Order order) {
        if (riskControlsProperties.isOrderEntryHalt()) {
            order.reject("ORDER_ENTRY_HALTED");
            throw new IllegalStateException("order entry halted");
        }

        if (riskControlsProperties.isReduceOnlyMode() && !order.isReduceOnly()) {
            order.reject("GLOBAL_REDUCE_ONLY");
            throw new IllegalStateException("global reduce-only mode enabled");
        }

        String symbol = order.getSymbol() == null ? "" : order.getSymbol().code();
        boolean suspended = riskControlsProperties.getSuspendedSymbols().stream()
                .map(RiskService::normalizeSymbol)
                .anyMatch(normalizeSymbol(symbol)::equals);
        if (suspended) {
            order.reject("SYMBOL_SUSPENDED");
            throw new IllegalStateException("symbol suspended");
        }
    }

    private void validateTickAndLot(Order order, SymbolConfig config) {
        if (order.getType() == OrderType.LIMIT && !isMultiple(order.getPrice(), config.priceTickOrDefault())) {
            order.reject("INVALID_PRICE_TICK");
            throw new IllegalArgumentException("price violates tick size");
        }
        if (!isMultiple(order.getQty(), config.lotSizeOrDefault())) {
            order.reject("INVALID_QTY_STEP");
            throw new IllegalArgumentException("qty violates lot size");
        }
        if (order.getQty().compareTo(config.minQtyOrDefault()) < 0) {
            order.reject("QTY_TOO_SMALL");
            throw new IllegalArgumentException("qty is below min qty");
        }
    }

    private void validateNotional(Order order, SymbolConfig config, BigDecimal referencePrice) {
        BigDecimal px = order.getType() == OrderType.LIMIT ? order.getPrice() : referencePrice;
        BigDecimal notional = px.multiply(order.getQty());
        if (notional.compareTo(config.minNotionalOrDefault()) < 0) {
            order.reject("MIN_NOTIONAL");
            throw new IllegalArgumentException("notional is below min notional");
        }
        if (notional.compareTo(config.maxOrderNotionalOrDefault()) > 0) {
            order.reject("MAX_ORDER_NOTIONAL");
            throw new IllegalArgumentException("order notional exceeds limit");
        }
    }

    private void validatePriceBand(Order order, SymbolConfig config) {
        if (order.getType() != OrderType.LIMIT) return;
        Optional<TopOfBook> top = matchingEngine.top(order.getSymbol().code());
        BigDecimal bestBid = top.map(TopOfBook::getBestBid).orElse(null);
        BigDecimal bestAsk = top.map(TopOfBook::getBestAsk).orElse(null);
        if (bestBid == null || bestAsk == null || bestBid.signum() <= 0 || bestAsk.signum() <= 0) return;

        BigDecimal mid = bestBid.add(bestAsk).divide(BigDecimal.valueOf(2), MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal deviation = order.getPrice().subtract(mid).abs()
                .divide(mid, MONEY_SCALE, RoundingMode.HALF_UP);
        if (deviation.compareTo(config.priceBandRateOrDefault()) > 0) {
            order.reject("PRICE_BAND_EXCEEDED");
            throw new IllegalArgumentException("price deviation exceeds symbol band");
        }
    }

    private void validateLeverage(Order order, SymbolConfig config) {
        if (order.getLeverage() <= 0 || order.getLeverage() > config.getMaxLeverage()) {
            order.reject("INVALID_LEVERAGE");
            throw new IllegalArgumentException("leverage exceeds symbol limit");
        }
    }

    private void validateMaxOpenOrders(Order order, SymbolConfig config) {
        int openOrderCount = orderRepo.findOpenOrders(order.getUid(), order.getSymbol().code()).size();
        if (openOrderCount >= config.maxOpenOrdersOrDefault()) {
            order.reject("MAX_OPEN_ORDERS");
            throw new IllegalStateException("max open orders exceeded");
        }
    }

    private void validateClientOrderIdDeduplication(Order order, UUID excludeOrderId) {
        String clientOrderId = normalizeClientOrderId(order.getClientOrderId());
        if (clientOrderId == null) return;

        boolean duplicated = orderRepo.findOpenOrders(order.getUid(), order.getSymbol().code()).stream()
                .filter(existing -> excludeOrderId == null || !excludeOrderId.equals(existing.getId()))
                .map(existing -> normalizeClientOrderId(existing.getClientOrderId()))
                .anyMatch(clientOrderId::equals);
        if (duplicated) {
            order.reject("DUPLICATE_CLIENT_ORDER_ID");
            throw new IllegalStateException("duplicate clientOrderId");
        }
    }

    private void validateReduceOnly(Order order) {
        if (!order.isReduceOnly()) return;
        Position position = positionRepo.find(order.getUid(), order.getSymbol()).orElse(null);
        if (position == null || position.getQty() == null || position.getQty().signum() == 0) {
            order.reject("REDUCE_ONLY_NO_POSITION");
            throw new IllegalArgumentException("reduce-only requires existing position");
        }
        if (order.getSide() == OrderSide.BUY && position.getQty().signum() > 0) {
            order.reject("REDUCE_ONLY_SIDE");
            throw new IllegalArgumentException("reduce-only BUY cannot reduce long position");
        }
        if (order.getSide() == OrderSide.SELL && position.getQty().signum() < 0) {
            order.reject("REDUCE_ONLY_SIDE");
            throw new IllegalArgumentException("reduce-only SELL cannot reduce short position");
        }
        if (order.getQty().compareTo(position.getQty().abs()) > 0) {
            order.reject("REDUCE_ONLY_QTY");
            throw new IllegalArgumentException("reduce-only qty exceeds position");
        }
    }

    private void validatePositionLimit(Order order, SymbolConfig config, BigDecimal referencePrice) {
        if (order.isReduceOnly()) return;
        Position position = positionRepo.find(order.getUid(), order.getSymbol()).orElse(null);
        BigDecimal currentQty = position == null || position.getQty() == null ? BigDecimal.ZERO : position.getQty();
        BigDecimal signedOrderQty = order.getSide() == OrderSide.BUY ? order.getQty() : order.getQty().negate();
        BigDecimal worstQty = currentQty.add(signedOrderQty).abs();
        BigDecimal worstNotional = worstQty.multiply(referencePrice);
        if (worstNotional.compareTo(config.maxPositionNotionalOrDefault()) > 0) {
            order.reject("MAX_POSITION_NOTIONAL");
            throw new IllegalArgumentException("position notional exceeds risk limit");
        }
    }

    private static boolean isOpen(Order order) {
        return order.getStatus() == Order.Status.NEW || order.getStatus() == Order.Status.PARTIALLY_FILLED;
    }

    private static boolean isMultiple(BigDecimal value, BigDecimal step) {
        if (value == null || step == null || step.signum() <= 0) return false;
        return value.divideAndRemainder(step)[1].compareTo(BigDecimal.ZERO) == 0;
    }

    private static String normalizeClientOrderId(String clientOrderId) {
        if (clientOrderId == null || clientOrderId.isBlank()) return null;
        return clientOrderId.trim();
    }

    private static String normalizeSymbol(String symbol) {
        return symbol == null ? "" : symbol.trim().toUpperCase();
    }
}
