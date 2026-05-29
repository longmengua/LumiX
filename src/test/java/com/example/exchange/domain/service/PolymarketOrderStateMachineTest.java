/*
 * 檔案用途：測試 Polymarket local/CLOB order 狀態轉換 guard。
 */
package com.example.exchange.domain.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
    @DisplayName("shouldApplyRemoteMatchedSize 對 stale active payload 保護 local terminal matched size")
    void shouldApplyRemoteMatchedSizeRejectsStaleActiveForTerminalLocalOrder() {
        assertThat(stateMachine.shouldApplyRemoteMatchedSize("ORDER_STATUS_FILLED", "ORDER_STATUS_MATCHED"))
                .isFalse();
        assertThat(stateMachine.shouldApplyRemoteMatchedSize("ORDER_STATUS_SETTLED", "live"))
                .isFalse();
        assertThat(stateMachine.shouldApplyRemoteMatchedSize("ORDER_STATUS_MATCHED", "ORDER_STATUS_MATCHED"))
                .isTrue();
    }
}
