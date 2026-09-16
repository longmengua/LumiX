# Phase 15 - Trading Runtime Core

## 狀態

```text
COMPLETED_FOR_TRADING_RUNTIME_CORE_FOUNDATION
```

## 目標

建立 Trading Runtime Core 的 scope gate 與 safety contracts，先定義 ledger、balance、reservation 與 settlement 的 runtime 邊界，再決定後續可安全前進的 sandbox 或 integration gate。

Phase 15 backend foundation gates completed
Phase 15 trading runtime core foundation completed
NOT production-ready
NOT full trading runtime
NOT order/matching/settlement ready
NOT reservation runtime ready
NOT settlement runtime ready
NOT futures/liquidation/withdrawal ready
NOT exchange ready
NOT public user trading ready

## 本 phase 可處理的最早工作

```text
ledger posting integration design
controlled ledger posting runtime gate
balance projection rebuild / read model design
balance projection rebuild runtime gate
reservation hold / release design
reconciliation design
reconciliation check design
no-go integration gate
```

## 不允許的 runtime

```text
真正過帳
真正更新餘額
真正 reservation hold / release
真正 settlement
真正交易下單
任何 production-ready 宣稱
```

## 高層 task list

```text
T01 trading runtime core scope gate and safety contracts
T02 ledger posting integration design gate
T03 balance projection runtime design gate
T04 ledger posting controlled runtime gate
T05 balance projection rebuild runtime gate
T06 reservation hold/release design gate
T07 reconciliation design gate
T08 trading runtime no-go integration gate
T09 phase 15 final review gate
```

## 文件索引

```text
docs/phases/PHASE_15_BALANCE_RECON/trading-runtime-core-scope.md
docs/phases/PHASE_15_BALANCE_RECON/trading-runtime-core-safety-contracts.md
docs/phases/PHASE_15_BALANCE_RECON/tasks/P15-T01.md
docs/phases/PHASE_15_BALANCE_RECON/ledger-posting-integration-design.md
docs/phases/PHASE_15_BALANCE_RECON/tasks/P15-T04.md
docs/phases/PHASE_15_BALANCE_RECON/balance-projection-runtime-design.md
docs/phases/PHASE_15_BALANCE_RECON/tasks/P15-T02.md
docs/phases/PHASE_15_BALANCE_RECON/tasks/P15-T03.md
docs/phases/PHASE_15_BALANCE_RECON/balance-projection-rebuild-gate.md
docs/phases/PHASE_15_BALANCE_RECON/tasks/P15-T05.md
docs/phases/PHASE_15_BALANCE_RECON/reservation-hold-release-design.md
docs/phases/PHASE_15_BALANCE_RECON/tasks/P15-T06.md
docs/phases/PHASE_15_BALANCE_RECON/reconciliation-design.md
docs/phases/PHASE_15_BALANCE_RECON/tasks/P15-T07.md
docs/phases/PHASE_15_BALANCE_RECON/trading-runtime-no-go-integration.md
docs/phases/PHASE_15_BALANCE_RECON/tasks/P15-T08.md
docs/phases/PHASE_15_BALANCE_RECON/phase-15-final-review.md
docs/phases/PHASE_15_BALANCE_RECON/tasks/P15-T09.md
```

## 運行邊界

```text
ledger append 是 source of truth 的候選執行路徑，但尚未正式接線
balance_projections 是 read model，不是 source of truth
reservations 是 hold / release runtime 的獨立邊界，不得偷渡進 ledger adapter
settlement 必須經過 ledger invariant、idempotency、append-only、reconciliation gate
```

## HUMAN_REVIEW_REQUIRED

```text
所有 money movement runtime 都屬於 HUMAN_REVIEW_REQUIRED。
所有 ledger / balance / reservation / settlement runtime 都屬於 HUMAN_REVIEW_REQUIRED。
所有 order / matching / futures / liquidation / withdrawal / security runtime 都屬於 HUMAN_REVIEW_REQUIRED。
所有 admin runtime 都屬於 HUMAN_REVIEW_REQUIRED。
任何把 Phase 15 誤寫成 production-ready 的行為都屬於 HUMAN_REVIEW_REQUIRED。
```

## 後續唯讀資產查詢 handoff

```text
ASSET-T01：COMPLETED_FOR_READ_ONLY_PROJECTION_API（2026-09-15）
```

`/api/v1/assets/balances` 僅以 authenticated session principal 的 userId 讀取既有 `balance_projections`，並回傳
account／asset metadata、精確十進位 amount、projection version、projectedAt、reconciledAt 與 freshness。它是 read model
presentation boundary，不會建立帳戶、重建 projection 或更新 ledger／balance／reservation；沒有 projection row 時回傳空集合，
不會以 mock 或假零餘額取代。此 handoff 不改變本 phase 的完成狀態，也不表示 balance mutation runtime 或正式資產功能已完成。

## 2026-09-15 資產 runtime：ledger-derived projection

`com.lumix.ledger.runtime.LedgerBalanceProjectionUpdater` 只接受已 append 的 immutable ledger entry identity，對每一個
受影響 USER `account_id + asset_symbol` 從 `ledger_entries` 重新加總，再於同一 posting transaction materialize
`balance_projections`。`V019__add_account_category_for_ledger_projection.sql` 將帳戶分為 USER 與 EXCHANGE，EXCHANGE
分錄不會變成使用者可見餘額。尚未有 reservation 時，available 等於 total、locked 為零，且 `reconciled_at` 保持 null；
這不是 reconciliation completed 宣稱。`HUMAN_REVIEW_REQUIRED`。
