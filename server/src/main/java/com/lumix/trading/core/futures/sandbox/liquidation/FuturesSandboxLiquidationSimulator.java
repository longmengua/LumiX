package com.lumix.trading.core.futures.sandbox.liquidation;

import com.lumix.common.MoneyAmount;
import com.lumix.trading.core.futures.position.FuturesPosition;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Phase 19-T01 的 pure、stateless liquidation simulator。
 *
 * 本 simulator 使用明確傳入的 collateral、mark price 與 maintenance rate，計算
 * `equity = collateral + unrealized PnL` 與 `maintenance = markPrice * quantity * rate`。
 * 它只輸出 simulation，絕不建立強平單、close fill、餘額異動、ledger entry 或 settlement。
 */
public final class FuturesSandboxLiquidationSimulator {

    /**
     * 以單次輸入試算 position 是否落在 maintenance margin 門檻。
     *
     * equity 等於 maintenance margin 時採保守的 simulated-liquidation 結論；這是 sandbox
     * comparison rule，不是 production liquidation priority、價格保護或破產價政策。
     */
    public FuturesSandboxLiquidationSimulationResult simulate(FuturesSandboxLiquidationSimulationRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        FuturesPosition position = request.position();
        BigDecimal unrealizedPnl = calculateUnrealizedPnl(position, request.markPrice().price());
        MoneyAmount equity = new MoneyAmount(request.simulatedCollateral().amount().value().add(unrealizedPnl));
        MoneyAmount maintenanceMargin = new MoneyAmount(request.markPrice().price()
                .multiply(position.quantity().value())
                .multiply(request.maintenanceMarginRate().value()));
        boolean liquidatable = equity.compareTo(maintenanceMargin) <= 0;

        return new FuturesSandboxLiquidationSimulationResult(
                position.positionId(),
                request.futuresAccount().settlementAsset(),
                request.markPrice(),
                request.simulatedCollateral(),
                request.maintenanceMarginRate(),
                equity,
                maintenanceMargin,
                liquidatable
                        ? FuturesSandboxLiquidationSimulationDecision.LIQUIDATION_SIMULATED
                        : FuturesSandboxLiquidationSimulationDecision.NOT_LIQUIDATABLE,
                liquidatable
                        ? FuturesSandboxLiquidationSimulationReason.EQUITY_AT_OR_BELOW_MAINTENANCE_MARGIN
                        : FuturesSandboxLiquidationSimulationReason.EQUITY_ABOVE_MAINTENANCE_MARGIN
        );
    }

    private static BigDecimal calculateUnrealizedPnl(FuturesPosition position, BigDecimal markPrice) {
        BigDecimal priceDifference = position.side().isLong()
                ? markPrice.subtract(position.entryPrice().value())
                : position.entryPrice().value().subtract(markPrice);
        return priceDifference.multiply(position.quantity().value());
    }
}
