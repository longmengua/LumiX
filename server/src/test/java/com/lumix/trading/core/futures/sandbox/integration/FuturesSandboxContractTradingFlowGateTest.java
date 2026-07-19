package com.lumix.trading.core.futures.sandbox.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.lumix.account.AccountId;
import com.lumix.account.AssetSymbol;
import com.lumix.common.MoneyAmount;
import com.lumix.common.RequestId;
import com.lumix.trading.core.futures.leverage.FuturesLeverage;
import com.lumix.trading.core.futures.order.FuturesOrderId;
import com.lumix.trading.core.futures.order.FuturesOrderSide;
import com.lumix.trading.core.futures.order.FuturesOrderStatus;
import com.lumix.trading.core.futures.order.FuturesOrderType;
import com.lumix.trading.core.futures.order.FuturesSandboxOrder;
import com.lumix.trading.core.futures.order.FuturesTimeInForce;
import com.lumix.trading.core.futures.position.FuturesEntryPrice;
import com.lumix.trading.core.futures.position.FuturesMarketSymbol;
import com.lumix.trading.core.futures.position.FuturesPositionId;
import com.lumix.trading.core.futures.position.FuturesPositionQuantity;
import com.lumix.trading.core.futures.sandbox.contract.FuturesSandboxContractEligibilityReason;
import com.lumix.trading.core.futures.sandbox.contract.FuturesSandboxContractEligibilityResult;
import com.lumix.trading.core.futures.sandbox.contract.FuturesSandboxContractInspection;
import com.lumix.trading.core.futures.sandbox.contract.RestrictedFuturesSandboxContract;
import com.lumix.trading.core.futures.sandbox.liquidation.FuturesSandboxLiquidationSimulationDecision;
import com.lumix.trading.core.futures.sandbox.liquidation.FuturesSandboxLiquidationSimulationReason;
import com.lumix.trading.core.futures.sandbox.liquidation.FuturesSandboxLiquidationSimulationResult;
import com.lumix.trading.core.futures.sandbox.liquidation.FuturesSandboxMaintenanceMarginRate;
import com.lumix.trading.core.futures.sandbox.liquidation.FuturesSandboxSimulatedCollateral;
import com.lumix.trading.core.futures.sandbox.market.FuturesSandboxMockMarkPrice;
import com.lumix.trading.core.futures.sandbox.market.FuturesSandboxMockMarkPriceSource;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** 驗證 P20 flow 遇到 contract 或風險拒絕時只停止在 sandbox gate。 */
class FuturesSandboxContractTradingFlowGateTest {
    private final FuturesSandboxContractTradingFlowGate gate = new FuturesSandboxContractTradingFlowGate();

    @Test
    void rejectsWhenContractInspectionWasRejected() {
        assertResult(
                gate.evaluate(contractRejected(), liquidation(false)),
                FuturesSandboxContractTradingFlowDecision.REJECTED,
                FuturesSandboxContractTradingFlowReason.CONTRACT_INSPECTION_REJECTED
        );
    }

    @Test
    void givesContractRejectionPriorityOverLiquidationSimulation() {
        assertResult(
                gate.evaluate(contractRejected(), liquidation(true)),
                FuturesSandboxContractTradingFlowDecision.REJECTED,
                FuturesSandboxContractTradingFlowReason.CONTRACT_INSPECTION_REJECTED
        );
    }

    @Test
    void rejectsEligibleContractWhenLiquidationWasSimulated() {
        assertResult(
                gate.evaluate(eligibleContract(), liquidation(true)),
                FuturesSandboxContractTradingFlowDecision.REJECTED,
                FuturesSandboxContractTradingFlowReason.LIQUIDATION_SIMULATED
        );
    }

    @Test
    void allowsOnlyEligibleContractWithNonLiquidatableSimulation() {
        assertResult(
                gate.evaluate(eligibleContract(), liquidation(false)),
                FuturesSandboxContractTradingFlowDecision.ELIGIBLE_FOR_SANDBOX_FLOW,
                FuturesSandboxContractTradingFlowReason.CONTRACT_AND_RISK_SIMULATION_ACCEPTED
        );
    }

    private static FuturesSandboxContractEligibilityResult contractRejected() {
        return FuturesSandboxContractEligibilityResult.rejected(
                FuturesSandboxContractEligibilityReason.ORDER_NOT_ACCEPTED_FOR_SANDBOX
        );
    }

    private static FuturesSandboxContractEligibilityResult eligibleContract() {
        FuturesMarketSymbol market = new FuturesMarketSymbol("BTC-USDT");
        FuturesSandboxOrder order = new FuturesSandboxOrder(
                new FuturesOrderId("order-btc-usdt"),
                new RequestId("request-btc-usdt"),
                new AccountId("futures-account"),
                market,
                FuturesOrderSide.BUY,
                FuturesOrderType.LIMIT,
                new FuturesPositionQuantity(new BigDecimal("2")),
                new FuturesEntryPrice(new BigDecimal("100")),
                FuturesTimeInForce.GTC,
                FuturesLeverage.of(10),
                Instant.EPOCH,
                FuturesOrderStatus.ACCEPTED_FOR_SANDBOX,
                Optional.empty()
        );
        FuturesSandboxMockMarkPrice mark = new FuturesSandboxMockMarkPrice(
                market,
                new BigDecimal("110"),
                FuturesSandboxMockMarkPriceSource.MANUAL_SANDBOX_INPUT,
                Instant.EPOCH
        );
        return FuturesSandboxContractEligibilityResult.eligible(
                new FuturesSandboxContractInspection(
                        new RestrictedFuturesSandboxContract(market),
                        order,
                        mark
                )
        );
    }

    private static FuturesSandboxLiquidationSimulationResult liquidation(boolean liquidated) {
        MoneyAmount maintenanceMargin = new MoneyAmount(BigDecimal.TEN);
        MoneyAmount simulatedEquity = new MoneyAmount(liquidated ? BigDecimal.TEN : new BigDecimal("11"));
        FuturesSandboxMockMarkPrice mark = new FuturesSandboxMockMarkPrice(
                new FuturesMarketSymbol("BTC-USDT"),
                new BigDecimal("100"),
                FuturesSandboxMockMarkPriceSource.MANUAL_SANDBOX_INPUT,
                Instant.EPOCH
        );
        return new FuturesSandboxLiquidationSimulationResult(
                new FuturesPositionId("position"),
                new AssetSymbol("USDT"),
                mark,
                new FuturesSandboxSimulatedCollateral(new MoneyAmount(BigDecimal.TEN)),
                new FuturesSandboxMaintenanceMarginRate(new BigDecimal("0.1")),
                maintenanceMargin,
                simulatedEquity,
                liquidated
                        ? FuturesSandboxLiquidationSimulationDecision.LIQUIDATION_SIMULATED
                        : FuturesSandboxLiquidationSimulationDecision.NOT_LIQUIDATABLE,
                liquidated
                        ? FuturesSandboxLiquidationSimulationReason.EQUITY_AT_OR_BELOW_MAINTENANCE_MARGIN
                        : FuturesSandboxLiquidationSimulationReason.EQUITY_ABOVE_MAINTENANCE_MARGIN
        );
    }

    private static void assertResult(
            FuturesSandboxContractTradingFlowResult actual,
            FuturesSandboxContractTradingFlowDecision decision,
            FuturesSandboxContractTradingFlowReason reason
    ) {
        assertEquals(decision, actual.decision());
        assertEquals(reason, actual.reason());
    }
}
