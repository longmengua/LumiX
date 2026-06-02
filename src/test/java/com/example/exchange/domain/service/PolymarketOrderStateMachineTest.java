/*
 * 檔案用途：測試 Polymarket local/CLOB order 狀態轉換 guard。
 */
package com.example.exchange.domain.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PolymarketOrderStateMachineTest {

    private final PolymarketOrderStateMachine stateMachine =
            new PolymarketOrderStateMachine();

    @Test
    @DisplayName("resolveRemoteStatus 不讓 stale active CLOB 狀態覆寫 local terminal 狀態")
    void resolveRemoteStatusKeepsLocalTerminalWhenRemoteIsStaleActive() {
        // 流程：trade/settlement 終態已落地後，較舊的 active order payload 只能保存 raw，不可倒退狀態。
        assertThat(stateMachine.resolveRemoteStatus("FILLED", "ORDER_STATUS_LIVE"))
                .isEqualTo("FILLED");
        assertThat(stateMachine.resolveRemoteStatus("ORDER_STATUS_SETTLED", "matched"))
                .isEqualTo("ORDER_STATUS_SETTLED");
    }

    @Test
    @DisplayName("resolveRemoteStatus 允許 terminal remote status 解除 local uncertain 或 active 狀態")
    void resolveRemoteStatusAllowsTerminalRemoteProgression() {
        assertThat(stateMachine.resolveRemoteStatus("CANCEL_OUTCOME_UNCERTAIN", "ORDER_STATUS_CANCELED"))
                .isEqualTo("ORDER_STATUS_CANCELED");
        assertThat(stateMachine.resolveRemoteStatus("ORDER_STATUS_MATCHED", "ORDER_STATUS_FILLED"))
                .isEqualTo("ORDER_STATUS_FILLED");
    }

    @Test
    @DisplayName("resolveRemoteStatus 不讓 late CLOB terminal payload 降級 settled 或 filled local order")
    void resolveRemoteStatusKeepsHigherPriorityTerminalLifecycle() {
        // 場景：settlement/fill 已先落地後，較晚抵達的 cancel/live payload 不應倒退 local lifecycle。
        assertThat(stateMachine.resolveRemoteStatus("ORDER_STATUS_SETTLED", "ORDER_STATUS_CANCELED"))
                .isEqualTo("ORDER_STATUS_SETTLED");
        assertThat(stateMachine.resolveRemoteStatus("ORDER_STATUS_FILLED", "ORDER_STATUS_CANCELED"))
                .isEqualTo("ORDER_STATUS_FILLED");
        assertThat(stateMachine.resolveRemoteStatus("ORDER_STATUS_FILLED", "ORDER_STATUS_SETTLED"))
                .isEqualTo("ORDER_STATUS_SETTLED");
    }

    @Test
    @DisplayName("shouldApplyRemoteMatchedSize 對 stale active payload 保護 local terminal matched size")
    void shouldApplyRemoteMatchedSizeRejectsStaleActiveForTerminalLocalOrder() {
        assertThat(stateMachine.shouldApplyRemoteMatchedSize("ORDER_STATUS_FILLED", "ORDER_STATUS_MATCHED"))
                .isFalse();
        assertThat(stateMachine.shouldApplyRemoteMatchedSize("ORDER_STATUS_SETTLED", "live"))
                .isFalse();
        assertThat(stateMachine.shouldApplyRemoteMatchedSize("ORDER_STATUS_SETTLED", "ORDER_STATUS_CANCELED"))
                .isFalse();
        assertThat(stateMachine.shouldApplyRemoteMatchedSize("ORDER_STATUS_MATCHED", "ORDER_STATUS_MATCHED"))
                .isTrue();
        assertThat(stateMachine.shouldApplyRemoteMatchedSize("ORDER_STATUS_FILLED", "ORDER_STATUS_SETTLED"))
                .isTrue();
    }

    @Test
    @DisplayName("transitionMatrix 明確列出 local/CLOB/trade/settlement lifecycle progression")
    void transitionMatrixDocumentsLocalClobTradeAndSettlementProgression() {
        // 場景：production review 需要能看見每個 lifecycle 來源如何推進或保護 local 狀態。
        assertThat(stateMachine.transitionMatrix())
                .extracting(PolymarketOrderStateMachine.TransitionRule::stage)
                .contains(
                        PolymarketOrderStateMachine.LifecycleStage.LOCAL,
                        PolymarketOrderStateMachine.LifecycleStage.CLOB_ORDER,
                        PolymarketOrderStateMachine.LifecycleStage.TRADE,
                        PolymarketOrderStateMachine.LifecycleStage.SETTLEMENT
                );
        assertThat(stateMachine.transitionMatrix())
                .anySatisfy(rule -> {
                    assertThat(rule.stage()).isEqualTo(PolymarketOrderStateMachine.LifecycleStage.CLOB_ORDER);
                    assertThat(rule.currentStatus()).isEqualTo("ORDER_STATUS_SETTLED");
                    assertThat(rule.protectsTerminalDowngrade()).isTrue();
                });
    }

    @Test
    @DisplayName("trade event 會推進 order lifecycle 並保留 trade status")
    void resolveUserEventStatusPromotesTradeMatch() {
        PolymarketOrderStateMachine.LifecycleTransition transition =
                stateMachine.resolveUserEventStatus("ORDER_STATUS_LIVE", null, "trade", "MATCHED");

        // 場景：user-channel trade event 比 CLOB reconcile 更早到，local order 應進入 matched lifecycle。
        assertThat(transition.stage()).isEqualTo(PolymarketOrderStateMachine.LifecycleStage.TRADE);
        assertThat(transition.orderStatus()).isEqualTo("ORDER_STATUS_MATCHED");
        assertThat(transition.tradeStatus()).isEqualTo("MATCHED");
        assertThat(transition.changed()).isTrue();
    }

    @Test
    @DisplayName("settlement event 會推進到 settled 並拒絕後續 active downgrade")
    void resolveUserEventStatusSettlesAndProtectsTerminalStatus() {
        PolymarketOrderStateMachine.LifecycleTransition settled =
                stateMachine.resolveUserEventStatus("ORDER_STATUS_MATCHED", "MATCHED", "settlement", "SETTLED");
        PolymarketOrderStateMachine.LifecycleTransition stale =
                stateMachine.resolveUserEventStatus(settled.orderStatus(), settled.tradeStatus(), "order", "ORDER_STATUS_LIVE");

        // 場景：settlement 已落地後，較舊的 order active event 不能把 local order 拉回 live。
        assertThat(settled.orderStatus()).isEqualTo("ORDER_STATUS_SETTLED");
        assertThat(settled.stage()).isEqualTo(PolymarketOrderStateMachine.LifecycleStage.SETTLEMENT);
        assertThat(stale.orderStatus()).isEqualTo("ORDER_STATUS_SETTLED");
    }

    @Test
    @DisplayName("settlement statuses 會把 matched 或 filled lifecycle 推進到 settled")
    void settlementStatusesPromoteMatchedOrFilledLifecycle() {
        // 場景：settlement/redeem 事件可能用不同 status 字串，但都應收斂成 local settled。
        for (String status : List.of("SETTLED", "ORDER_STATUS_SETTLED", "SETTLEMENT_CONFIRMED", "REDEEMED")) {
            assertThat(stateMachine.resolveUserEventStatus("ORDER_STATUS_MATCHED", "MATCHED", "settlement", status)
                    .orderStatus())
                    .isEqualTo("ORDER_STATUS_SETTLED");
            assertThat(stateMachine.resolveUserEventStatus("ORDER_STATUS_FILLED", "MATCHED", "settlement", status)
                    .orderStatus())
                    .isEqualTo("ORDER_STATUS_SETTLED");
        }
    }

    @Test
    @DisplayName("settlement event 不會覆寫 canceled/rejected/failed terminal local order")
    void settlementEventDoesNotRewriteNonSettlementTerminalOrders() {
        // 場景：已取消或失敗的 local order 即使收到 settlement-shaped payload，也不可被錯誤關成 settled。
        for (String status : List.of("ORDER_STATUS_CANCELED", "REJECTED", "FAILED")) {
            PolymarketOrderStateMachine.LifecycleTransition transition =
                    stateMachine.resolveUserEventStatus(status, null, "settlement", "REDEEMED");

            assertThat(transition.orderStatus())
                    .isEqualTo(status);
            assertThat(transition.changed())
                    .isFalse();
        }
    }

    @Test
    @DisplayName("settled terminal order 不接受後續 trade 或 terminal CLOB downgrade")
    void settledTerminalOrderRejectsLaterTradeOrClobDowngrade() {
        PolymarketOrderStateMachine.LifecycleTransition tradeReplay =
                stateMachine.resolveUserEventStatus("ORDER_STATUS_SETTLED", "MATCHED", "trade", "MATCHED");
        PolymarketOrderStateMachine.LifecycleTransition lateCancel =
                stateMachine.resolveUserEventStatus("ORDER_STATUS_SETTLED", "MATCHED", "order", "ORDER_STATUS_CANCELED");

        // 場景：user-channel 與 CLOB reconcile 可能亂序；settled 是最高優先級終態，不可被 replay 倒退。
        assertThat(tradeReplay.orderStatus())
                .isEqualTo("ORDER_STATUS_SETTLED");
        assertThat(tradeReplay.changed())
                .isFalse();
        assertThat(lateCancel.orderStatus())
                .isEqualTo("ORDER_STATUS_SETTLED");
        assertThat(lateCancel.changed())
                .isFalse();
    }
}
