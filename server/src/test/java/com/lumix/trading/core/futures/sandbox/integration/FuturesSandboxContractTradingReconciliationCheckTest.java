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

/** 驗證 P20-T03 只以 immutable 輸入重放並檢查 flow result，不會修正任何資料。 */
class FuturesSandboxContractTradingReconciliationCheckTest {
    private final FuturesSandboxContractTradingFlowGate flowGate = new FuturesSandboxContractTradingFlowGate();
    private final FuturesSandboxContractTradingReconciliationCheck reconciliationCheck =
            new FuturesSandboxContractTradingReconciliationCheck();

    @Test
    void marksRecordedSafeFlowAsConsistentWhenReplayMatches() {
        FuturesSandboxContractEligibilityResult contract = eligibleContract();
        FuturesSandboxLiquidationSimulationResult liquidation = liquidation(false);
        FuturesSandboxContractTradingFlowResult recorded = flowGate.evaluate(contract, liquidation);

        FuturesSandboxContractTradingReconciliationResult result =
                reconciliationCheck.check(contract, liquidation, recorded);

        assertEquals(FuturesSandboxContractTradingReconciliationDecision.CONSISTENT, result.decision());
        assertEquals(FuturesSandboxContractTradingReconciliationReason.REPLAY_MATCHES_RECORDED_FLOW, result.reason());
        assertEquals(recorded, result.replayedFlow());
        assertEquals(recorded, result.recordedFlow());
    }

    @Test
    void detectsRecordedEligibilityThatContradictsRejectedContractInspection() {
        FuturesSandboxContractEligibilityResult contract = FuturesSandboxContractEligibilityResult.rejected(
                FuturesSandboxContractEligibilityReason.ORDER_NOT_ACCEPTED_FOR_SANDBOX
        );
        FuturesSandboxLiquidationSimulationResult liquidation = liquidation(false);

        FuturesSandboxContractTradingReconciliationResult result = reconciliationCheck.check(
                contract,
                liquidation,
                FuturesSandboxContractTradingFlowResult.eligible()
        );

        assertEquals(FuturesSandboxContractTradingReconciliationDecision.MISMATCH_DETECTED, result.decision());
        assertEquals(FuturesSandboxContractTradingReconciliationReason.REPLAY_DIFFERS_FROM_RECORDED_FLOW, result.reason());
        assertEquals(FuturesSandboxContractTradingFlowReason.CONTRACT_INSPECTION_REJECTED, result.replayedFlow().reason());
        assertEquals(FuturesSandboxContractTradingFlowReason.CONTRACT_AND_RISK_SIMULATION_ACCEPTED, result.recordedFlow().reason());
    }

    @Test
    void detectsRecordedContractRejectionThatContradictsLiquidationSimulation() {
        FuturesSandboxContractTradingReconciliationResult result = reconciliationCheck.check(
                eligibleContract(),
                liquidation(true),
                FuturesSandboxContractTradingFlowResult.rejected(
                        FuturesSandboxContractTradingFlowReason.CONTRACT_INSPECTION_REJECTED
                )
        );

        assertEquals(FuturesSandboxContractTradingReconciliationDecision.MISMATCH_DETECTED, result.decision());
        assertEquals(FuturesSandboxContractTradingFlowReason.LIQUIDATION_SIMULATED, result.replayedFlow().reason());
        assertEquals(FuturesSandboxContractTradingFlowReason.CONTRACT_INSPECTION_REJECTED, result.recordedFlow().reason());
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
        return new FuturesSandboxLiquidationSimulationResult(
                new FuturesPositionId("position"),
                new AssetSymbol("USDT"),
                new FuturesSandboxMockMarkPrice(
                        new FuturesMarketSymbol("BTC-USDT"),
                        new BigDecimal("100"),
                        FuturesSandboxMockMarkPriceSource.MANUAL_SANDBOX_INPUT,
                        Instant.EPOCH
                ),
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
}
