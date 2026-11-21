package com.lumix.trading.runtime;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** 資金費用以 USDT 在雙向持倉間零和分配；正費率 LONG 支付 SHORT。 */
public final class FuturesFundingAllocator {
    private FuturesFundingAllocator() { }

    public record Position(String accountId, String side, BigDecimal quantity,
                           BigDecimal isolatedCollateralUsdt) { }
    public record Allocation(String accountId, String side, BigDecimal fundingUsdt,
                             BigDecimal resultingCollateralUsdt) { }

    public static List<Allocation> allocate(List<Position> positions, BigDecimal markPrice,
                                            BigDecimal fundingRate, BigDecimal maintenanceRate,
                                            int usdtScale, RoundingMode rounding) {
        Objects.requireNonNull(positions, "positions are required");
        Objects.requireNonNull(markPrice, "mark price is required");
        Objects.requireNonNull(fundingRate, "funding rate is required");
        Objects.requireNonNull(maintenanceRate, "maintenance rate is required");
        Objects.requireNonNull(rounding, "funding rounding is required");
        if (markPrice.signum() <= 0 || maintenanceRate.signum() <= 0
                || usdtScale < 0 || usdtScale > 18 || rounding == RoundingMode.UNNECESSARY)
            throw new IllegalArgumentException("funding valuation policy is invalid");
        List<Position> sorted = positions.stream().sorted(Comparator.comparing(Position::accountId)
                .thenComparing(Position::side)).toList();
        BigDecimal longQty = BigDecimal.ZERO;
        BigDecimal shortQty = BigDecimal.ZERO;
        for (Position position : sorted) {
            if (position.accountId() == null || position.accountId().isBlank()
                    || position.quantity() == null || position.quantity().signum() <= 0
                    || position.isolatedCollateralUsdt() == null
                    || position.isolatedCollateralUsdt().signum() < 0)
                throw new IllegalArgumentException("funding position is invalid");
            if ("LONG".equals(position.side())) longQty = longQty.add(position.quantity());
            else if ("SHORT".equals(position.side())) shortQty = shortQty.add(position.quantity());
            else throw new IllegalArgumentException("funding position side is invalid");
        }
        if (longQty.compareTo(shortQty) != 0)
            throw new TradingOrderRejectedException("long and short open interest is not reconciled");
        if (sorted.isEmpty() || fundingRate.signum() == 0) return List.of();
        String payerSide = fundingRate.signum() > 0 ? "LONG" : "SHORT";
        List<Allocation> result = new ArrayList<>();
        BigDecimal totalPaid = BigDecimal.ZERO;
        for (Position position : sorted) {
            if (!payerSide.equals(position.side())) continue;
            BigDecimal payment = position.quantity().multiply(markPrice).multiply(fundingRate.abs())
                    .setScale(usdtScale, rounding);
            BigDecimal remaining = position.isolatedCollateralUsdt().subtract(payment);
            BigDecimal maintenance = position.quantity().multiply(markPrice).multiply(maintenanceRate)
                    .setScale(usdtScale, RoundingMode.CEILING);
            if (remaining.compareTo(maintenance) < 0)
                throw new TradingOrderRejectedException("funding payer requires liquidation before settlement");
            totalPaid = totalPaid.add(payment);
            result.add(new Allocation(position.accountId(), position.side(), payment.negate(), remaining));
        }
        List<Position> receivers = sorted.stream().filter(position -> !payerSide.equals(position.side())).toList();
        BigDecimal remainingPayout = totalPaid;
        BigDecimal receiverQty = payerSide.equals("LONG") ? shortQty : longQty;
        for (int index = 0; index < receivers.size(); index++) {
            Position position = receivers.get(index);
            BigDecimal payout = index == receivers.size() - 1 ? remainingPayout
                    : totalPaid.multiply(position.quantity()).divide(receiverQty, usdtScale, RoundingMode.DOWN);
            remainingPayout = remainingPayout.subtract(payout);
            result.add(new Allocation(position.accountId(), position.side(), payout,
                    position.isolatedCollateralUsdt()));
        }
        return result.stream().sorted(Comparator.comparing(Allocation::accountId)
                .thenComparing(Allocation::side)).toList();
    }
}
