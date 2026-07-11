package com.lumix.trading.core.projection;

import java.util.List;

/**
 * balance projection runtime 的設計政策。
 *
 * 這個 policy 只描述 read model 的 rebuild / replay / reconcile 語意，不會直接更新 balance_projections。
 */
public final class BalanceProjectionRuntimePolicy {

    /**
     * 建立 balance projection runtime 的設計契約。
     *
     * 這份輸出只描述從 ledger 推導 read model 的方式，不代表已經有正式 projection runtime。
     */
    public BalanceProjectionRuntimeDesign describe() {
        return new BalanceProjectionRuntimeDesign(
                List.of(
                        BalanceProjectionCapability.READ_MODEL_ONLY,
                        BalanceProjectionCapability.REBUILD_FROM_LEDGER,
                        BalanceProjectionCapability.REPLAY_FROM_LEDGER,
                        BalanceProjectionCapability.RECONCILE_WITH_LEDGER,
                        BalanceProjectionCapability.OBSERVE_LAG
                ),
                List.of(
                        "ledger 是 source of truth",
                        "balance_projections 是 read model",
                        "projection 可以 rebuild / replay / reconcile",
                        "projection runtime 未來必須能從 ledger_entries 推導 total / available / locked",
                        "CREDIT 增加 total_amount，DEBIT 減少 total_amount",
                        "目前 rebuild gate 只 materialize SPOT account 的 read model rows",
                        "projected row 以 account_id + asset_symbol 為單位"
                ),
                List.of(
                        "projection lag 必須可觀測",
                        "projection mismatch 必須進 reconciliation flow",
                        "available_amount 在 reservation runtime 完成前等於 total_amount",
                        "locked_amount 在 reservation runtime 完成前固定為 0"
                ),
                List.of(
                        "不允許直接 insert / update / delete balance_projections runtime SQL",
                        "不把 balance_projections 當作資金真相來源",
                        "不讓 order / matching / settlement 直接把 balance_projections 當成 source of truth",
                        "不把負數 total_amount 當成可接受的 projection 結果",
                        "不新增正式 projection runtime 服務",
                        "不新增正式 balance mutation runtime",
                        "不新增資料存取層元件",
                        "不新增交易邊界"
                ),
                List.of(
                        "total",
                        "available",
                        "locked"
                )
        );
    }
}
