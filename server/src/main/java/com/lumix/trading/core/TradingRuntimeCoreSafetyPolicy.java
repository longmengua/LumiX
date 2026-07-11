package com.lumix.trading.core;

import java.util.List;

/**
 * Trading Runtime Core 的安全契約政策。
 *
 * 這個 policy 只整理 scope 與 safety contracts，不會接任何 repository、transaction 或 database client。
 */
public final class TradingRuntimeCoreSafetyPolicy {

    /**
     * 建立 Trading Runtime Core 的設計契約。
     *
     * 這份輸出只服務 phase 15 的 scope gate，不代表已經可以執行正式 money movement。
     */
    public TradingRuntimeCoreSafetyContract describe() {
        return new TradingRuntimeCoreSafetyContract(
                List.of(
                        TradingRuntimeCoreScope.LEDGER_POSTING_INTEGRATION_GATE,
                        TradingRuntimeCoreScope.BALANCE_PROJECTION_REBUILD_DESIGN,
                        TradingRuntimeCoreScope.RESERVATION_HOLD_RELEASE_DESIGN,
                        TradingRuntimeCoreScope.BASIC_RECONCILIATION_DESIGN
                ),
                List.of(
                        "ledger append 是 source of truth 的候選執行路徑，但尚未正式接線",
                        "balance_projections 是 read model，不是 source of truth",
                        "reservations 是 hold / release runtime 的獨立邊界，不得偷渡進 ledger adapter",
                        "settlement 必須經過 ledger invariant、idempotency、append-only、reconciliation gate"
                ),
                List.of(
                        "所有 amount / price / quantity 必須 BigDecimal，不得 float / double",
                        "所有 money movement 必須 HUMAN_REVIEW_REQUIRED",
                        "requestId 不等於 idempotency guarantee",
                        "idempotency key 才能代表 duplicate prevention contract",
                        "ledger append 成功不代表 balance projection 已同步",
                        "balance projection 不可作為資金真相來源",
                        "reservation 不可直接改 ledger，必須經 application boundary",
                        "settlement 必須是 explicit process，不可由 matching 或 order runtime 偷寫 ledger",
                        "所有正式 DB write 必須有 rollback / reconciliation 測試",
                        "所有高風險 runtime 不得 auto-commit"
                ),
                List.of(
                        "真正過帳",
                        "真正更新餘額",
                        "真正 reservation hold / release",
                        "真正 settlement",
                        "真正交易下單",
                        "任何 production-ready 宣稱"
                ),
                List.of(
                        "ledger posting integration design",
                        "balance projection rebuild / read model design",
                        "reservation hold / release design",
                        "reconciliation check design"
                )
        );
    }
}
