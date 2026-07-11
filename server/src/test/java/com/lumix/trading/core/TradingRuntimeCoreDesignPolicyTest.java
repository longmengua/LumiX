package com.lumix.trading.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lumix.trading.core.posting.LedgerPostingIntegrationStep;
import com.lumix.trading.core.projection.BalanceProjectionCapability;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 驗證 Trading Runtime Core 的 design policy 只輸出設計契約，不會變成正式 runtime。
 */
class TradingRuntimeCoreDesignPolicyTest {

    private final TradingRuntimeCoreSafetyPolicy policy = new TradingRuntimeCoreSafetyPolicy();

    /**
     * 確認 ledger posting integration 的順序與限制符合 phase 15 的設計門檻。
     *
     * 這個 case 必須存在，因為正式接線前的順序一旦錯亂，後續 money movement 就會失去安全邊界。
     */
    @Test
    void ledgerPostingIntegrationOrderAndRestrictionsAreExplicit() {
        var design = policy.describeLedgerPostingIntegration();

        assertEquals(
                List.of(
                        LedgerPostingIntegrationStep.REQUEST_IDENTITY_AND_IDEMPOTENCY,
                        LedgerPostingIntegrationStep.PREREQUISITE_GATE,
                        LedgerPostingIntegrationStep.LEDGER_INVARIANT_CHECK,
                        LedgerPostingIntegrationStep.APPEND_TRANSACTION_BOUNDARY,
                        LedgerPostingIntegrationStep.APPEND_LEDGER_ROWS,
                        LedgerPostingIntegrationStep.OUTBOX_APPEND,
                        LedgerPostingIntegrationStep.AUDIT_APPEND,
                        LedgerPostingIntegrationStep.RECONCILIATION_MARKER
                ),
                design.steps()
        );
        assertTrue(design.integrationRules().stream().anyMatch(text -> text.contains("requestId 不等於完整 idempotency guarantee")));
        assertTrue(design.integrationRules().stream().anyMatch(text -> text.contains("accepted posting plan 不等於 posted / committed / persisted")));
        assertTrue(design.noGoConditions().stream().anyMatch(text -> text.contains("正式 posting runtime")));
        assertTrue(design.noGoConditions().stream().anyMatch(text -> text.contains("資料存取層元件")));
        assertFalse(design.noGoConditions().stream().anyMatch(text -> text.contains("production-ready")));
    }

    /**
     * 確認 balance projection 只被定義成 read model，並且具備 rebuild / replay / reconcile 語意。
     *
     * 這個 case 必須存在，因為 balance projection 若被誤寫成資金真相，就會直接破壞對帳與風險控制。
     */
    @Test
    void balanceProjectionIsReadModelOnlyAndRebuildable() {
        var design = policy.describeBalanceProjectionRuntime();

        assertTrue(design.capabilities().contains(BalanceProjectionCapability.READ_MODEL_ONLY));
        assertTrue(design.capabilities().contains(BalanceProjectionCapability.REBUILD_FROM_LEDGER));
        assertTrue(design.capabilities().contains(BalanceProjectionCapability.REPLAY_FROM_LEDGER));
        assertTrue(design.capabilities().contains(BalanceProjectionCapability.RECONCILE_WITH_LEDGER));
        assertTrue(design.capabilities().contains(BalanceProjectionCapability.OBSERVE_LAG));
        assertTrue(design.sourceOfTruthRules().stream().anyMatch(text -> text.contains("ledger 是 source of truth")));
        assertTrue(design.sourceOfTruthRules().stream().anyMatch(text -> text.contains("balance_projections 是 read model")));
        assertTrue(design.observabilityRules().stream().anyMatch(text -> text.contains("lag 必須可觀測")));
        assertTrue(design.noGoConditions().stream().anyMatch(text -> text.contains("不允許直接 insert / update / delete balance_projections runtime SQL")));
        assertTrue(design.noGoConditions().stream().anyMatch(text -> text.contains("正式 projection runtime")));
        assertTrue(design.noGoConditions().stream().anyMatch(text -> text.contains("交易邊界")));
        assertTrue(design.derivedBalanceFields().contains("total"));
        assertTrue(design.derivedBalanceFields().contains("available"));
        assertTrue(design.derivedBalanceFields().contains("locked"));
    }

    /**
     * 確認 design policy 不會把已接受的 plan 誤包成已完成的 posting 結果。
     *
     * 這個 case 必須存在，因為 accepted plan 只能表示設計門檻過關，不能表示已經寫入或已結算。
     */
    @Test
    void acceptedPlanIsNotPostedOrCommitted() {
        var postingDesign = policy.describeLedgerPostingIntegration();

        assertTrue(postingDesign.integrationRules().stream().anyMatch(text -> text.contains("accepted posting plan 不等於 posted / committed / persisted")));
        assertFalse(postingDesign.integrationRules().stream().anyMatch(text -> text.contains("posted = true")));
    }
}
