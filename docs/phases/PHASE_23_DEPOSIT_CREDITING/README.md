# Phase 23 - 入金入帳與確認政策

## 狀態

```text
IN_PROGRESS_P23_T01_COMPLETED
```

P23-T01 的純 candidate/policy decision contract 已完成；不會 credit、寫 ledger、改 balance 或連任何 chain/provider。逐卡 approve 機制依人類指示暫停。

## 目標與依賴

把 P22 的唯讀鏈上 observation 轉成可稽核的 confirmation/credit decision，再以明確、冪等的受控 handoff 接到既有 ledger boundary。P22 foundation 已完成；逐卡 approve 暫停不解除資金與 ledger safety boundary。

## 詳細 task draft

| Draft | 目標與交付 | 禁止事項與驗收 |
| --- | --- | --- |
| P23-T01 | 定義 deposit candidate、confirmation threshold、asset/network policy、decision reason 與 versioning | COMPLETED；不做 credit；已測試不足確認、錯 asset/network、policy version mismatch |
| P23-T02 | 定義 credit idempotency key、immutable decision record 與 ledger posting handoff boundary | 不直接改 balance；測試 duplicate、concurrent retry、同 key 異 payload、overflow/precision |
| P23-T03 | 定義 reorg 後的 freeze/reversal/escalation decision，僅允許 append-only correction 路徑 | 不刪改既有 entry；測試 confirmed 後 reorg、reversal ordering、人工升級 |
| P23-T04 | 定義 deposit-to-ledger-to-balance reconciliation evidence、exception queue 與 audit export input | 不實作 repair/admin command；驗收為可重放差異報告與 fail-closed exception |

## 核心不變式與風險

- credit 的唯一性不可只依 transaction hash；必須綁定 chain/network、來源事件索引、asset、owner 與 policy version。
- 未達 finality、資料衝突、reorg、未知 asset、精度/overflow 或 reconciliation mismatch 一律不得自動 credit。
- 修正只能 append reversal/adjustment，不能覆寫或刪除 credit history。
- P23 所有 runtime 卡均 `HUMAN_REVIEW_REQUIRED`，且需專門審查 ledger/balance atomicity、idempotency、reorg 與營運升級流程。

## 停止條件與下一步

任何自動修復、admin balance adjustment、provider 連線或未審核 migration 需求均停止。P24 仍是獨立提款路徑，不得因入金 credit 設計而取得實作授權。

## P23-T01 實作紀錄

`com.lumix.deposit.credit` 定義 versioned asset/network policy、P22 evidence 與 address ownership 組成的 candidate，以及 deterministic eligibility decision。policy 必須同時驗證 policy version、network、asset、recipient address、active ownership、finality lifecycle 與確認數；任一不符都以具名 reason fail-closed。`ELIGIBLE_FOR_FUTURE_HANDOFF` 不會執行或授權任何 ledger posting、balance mutation 或 credit。
