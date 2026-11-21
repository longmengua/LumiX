package com.lumix.trading.core.futures.position;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * 雙向持倉的單次成交計算。此純規則不處理抵押幣別、匯率或帳本入帳，
 * 呼叫端須在同一交易內完成保證金、損益與持倉狀態更新。
 */
public final class FuturesHedgePositionTransition {
    private FuturesHedgePositionTransition() { }

    /** 同一帳戶與商品可各有一筆 LONG、SHORT，數量不能以正負號暗示方向。 */
    public record Position(FuturesPositionSide side, BigDecimal quantity, BigDecimal entryNotionalUsdt) {
        public Position {
            Objects.requireNonNull(side, "position side is required");
            quantity = nonNegative(quantity, "position quantity");
            if (quantity.scale() < 0 || quantity.scale() > 18 || quantity.precision() > 38)
                throw new IllegalArgumentException("position quantity exceeds schema precision");
            if (quantity.signum() == 0) {
                if (entryNotionalUsdt != null && entryNotionalUsdt.signum() != 0)
                    throw new IllegalArgumentException("closed position cannot retain entry cost");
            } else {
                entryNotionalUsdt = positive(entryNotionalUsdt, "entry notional");
                if (entryNotionalUsdt.scale() < 0 || entryNotionalUsdt.scale() > 36
                        || entryNotionalUsdt.precision() > 76)
                    throw new IllegalArgumentException("entry notional exceeds schema precision");
            }
        }
    }

    /** 已實現損益以報價幣計價，須由外層政策決定結算與入帳幣別。 */
    public record Result(Position position, BigDecimal realizedPnl) { }

    public enum OrderSide { BUY, SELL }

    /**
     * 成交必須明確指定 LONG 或 SHORT。LONG 買入、SHORT 賣出為開倉方向，
     * 反向成交只減少指定方向的持倉，不會隱含轉入另一方向。
     */
    public static Result apply(Position current, FuturesPositionSide positionSide, OrderSide orderSide,
                               BigDecimal fillQuantity, BigDecimal fillPrice, boolean reduceOnly,
                               int pnlScale, RoundingMode pnlRounding) {
        Objects.requireNonNull(positionSide, "position side is required");
        Objects.requireNonNull(orderSide, "order side is required");
        fillQuantity = positive(fillQuantity, "fill quantity");
        fillPrice = positive(fillPrice, "fill price");
        if (fillQuantity.scale() < 0 || fillQuantity.scale() > 18 || fillQuantity.precision() > 38
                || fillPrice.scale() < 0 || fillPrice.scale() > 18 || fillPrice.precision() > 38)
            throw new IllegalArgumentException("fill exceeds schema precision");
        if (pnlScale < 0 || pnlScale > 18) throw new IllegalArgumentException("PnL scale is invalid");
        Objects.requireNonNull(pnlRounding, "PnL rounding is required");
        Position before = current == null ? new Position(positionSide, BigDecimal.ZERO, null) : current;
        if (before.side() != positionSide) throw new IllegalArgumentException("position side mismatch");

        boolean opening = positionSide == FuturesPositionSide.LONG
                ? orderSide == OrderSide.BUY : orderSide == OrderSide.SELL;
        if (opening) {
            if (reduceOnly) throw new IllegalArgumentException("reduce-only order cannot increase exposure");
            BigDecimal nextQuantity = before.quantity().add(fillQuantity);
            BigDecimal nextCost = before.quantity().signum() == 0 ? fillPrice.multiply(fillQuantity)
                    : before.entryNotionalUsdt().add(fillPrice.multiply(fillQuantity));
            return new Result(new Position(positionSide, nextQuantity, nextCost), BigDecimal.ZERO);
        }
        if (before.quantity().compareTo(fillQuantity) < 0)
            throw new IllegalArgumentException("close quantity exceeds position");
        // 部分平倉按數量分攤精確成本，最後一筆直接拿剩餘成本，避免平均價取整殘留成本。
        BigDecimal allocatedCost = before.quantity().compareTo(fillQuantity) == 0
                ? before.entryNotionalUsdt()
                : before.entryNotionalUsdt().multiply(fillQuantity).divide(before.quantity(), pnlScale, pnlRounding);
        BigDecimal proceeds = fillPrice.multiply(fillQuantity);
        BigDecimal pnl = positionSide == FuturesPositionSide.LONG
                ? proceeds.subtract(allocatedCost) : allocatedCost.subtract(proceeds);
        BigDecimal remaining = before.quantity().subtract(fillQuantity);
        BigDecimal settledPnl = pnl.setScale(pnlScale, pnlRounding);
        if (settledPnl.precision() > 36) throw new IllegalArgumentException("PnL exceeds ledger precision");
        return new Result(new Position(positionSide, remaining,
                remaining.signum() == 0 ? null : before.entryNotionalUsdt().subtract(allocatedCost)),
                settledPnl);
    }

    private static BigDecimal nonNegative(BigDecimal value, String name) {
        Objects.requireNonNull(value, name + " is required");
        if (value.signum() < 0) throw new IllegalArgumentException(name + " must be non-negative");
        return value;
    }

    private static BigDecimal positive(BigDecimal value, String name) {
        nonNegative(value, name);
        if (value.signum() == 0) throw new IllegalArgumentException(name + " must be positive");
        return value;
    }
}
