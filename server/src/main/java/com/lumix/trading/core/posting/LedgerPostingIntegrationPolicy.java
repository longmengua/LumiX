package com.lumix.trading.core.posting;

import java.util.List;

/**
 * ledger posting integration 的設計政策。
 *
 * 這個 policy 只整理順序與限制，不會連到 JDBC、repository 或任何 money movement runtime。
 */
public final class LedgerPostingIntegrationPolicy {

    /**
     * 建立正式 ledger posting integration 的設計契約。
     *
     * 這份輸出只描述未來接線順序，不代表已經可用於正式過帳。
     */
    public LedgerPostingIntegrationDesign describe() {
        return new LedgerPostingIntegrationDesign(
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
                List.of(
                        "requestId 不等於完整 idempotency guarantee",
                        "accepted posting plan 不等於 posted / committed / persisted",
                        "LedgerAppendOnlyJdbcAdapter 不得被直接從 API / controller 呼叫",
                        "所有正式 posting integration 都是 HUMAN_REVIEW_REQUIRED"
                ),
                List.of(
                        "不接正式 ledger posting runtime",
                        "不把 accepted plan 當成已完成資金異動",
                        "不新增正式 posting runtime 服務",
                        "不新增資料存取層元件",
                        "不新增交易邊界",
                        "不新增 DB client call",
                        "不更新 balance_projections"
                )
        );
    }
}
