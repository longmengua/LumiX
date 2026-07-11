package com.lumix.trading.core;

import com.lumix.trading.core.posting.LedgerPostingIntegrationDesign;
import com.lumix.trading.core.posting.LedgerPostingIntegrationPolicy;
import com.lumix.trading.core.projection.BalanceProjectionRuntimeDesign;
import com.lumix.trading.core.projection.BalanceProjectionRuntimePolicy;
import com.lumix.trading.core.reservation.ReservationHoldReleaseDesign;
import com.lumix.trading.core.reservation.ReservationHoldReleaseDesignPolicy;

import java.util.List;

/**
 * Trading Runtime Core 的安全契約政策。
 *
 * 這個 policy 只整理 scope 與 safety contracts，不會接任何 repository、transaction 或 database client。
 */
public final class TradingRuntimeCoreSafetyPolicy {

    private final LedgerPostingIntegrationPolicy ledgerPostingIntegrationPolicy = new LedgerPostingIntegrationPolicy();
    private final BalanceProjectionRuntimePolicy balanceProjectionRuntimePolicy = new BalanceProjectionRuntimePolicy();
    private final ReservationHoldReleaseDesignPolicy reservationHoldReleaseDesignPolicy = new ReservationHoldReleaseDesignPolicy();

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
                        "reservation hold 會降低 available_amount，release 會增加 available_amount",
                        "reservation commit 必須經 settlement / ledger posting gate",
                        "order intake 可以要求 reservation，但不得直接寫 reservation DB",
                        "matching 不得偷寫 reservation 或 balance_projections",
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
                        "reservation hold / release 必須先做 idempotency decision，再做 hold / release",
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
                        "所有 reservation runtime 都屬於 HUMAN_REVIEW_REQUIRED",
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

    /**
     * 建立 ledger posting integration 的設計契約。
     *
     * 這只描述正式接線前必須遵守的順序，不代表可以直接過帳。
     */
    public LedgerPostingIntegrationDesign describeLedgerPostingIntegration() {
        return ledgerPostingIntegrationPolicy.describe();
    }

    /**
     * 建立 balance projection runtime 的設計契約。
     *
     * 這只描述 read model 如何 rebuild / replay / reconcile，不代表可以直接修改 balance_projections。
     */
    public BalanceProjectionRuntimeDesign describeBalanceProjectionRuntime() {
        return balanceProjectionRuntimePolicy.describe();
    }

    /**
     * 建立 reservation hold/release 的設計契約。
     *
     * 這只描述 hold / release / commit / cancel 的邊界，不代表可以直接操作 reservation DB。
     */
    public ReservationHoldReleaseDesign describeReservationHoldReleaseDesign() {
        return reservationHoldReleaseDesignPolicy.describe();
    }
}
