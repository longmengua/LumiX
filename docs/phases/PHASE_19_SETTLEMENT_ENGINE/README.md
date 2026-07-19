# Phase 19 - Risk Sandbox

## 狀態

```text
in progress — T01-T05 completed; HUMAN_REVIEW_REQUIRED final review pending
```

## 目標

建立 risk sandbox，先做 liquidation simulation、funding mock、insurance fund placeholder 與 risk / reconciliation tests。

## Sandbox 內容

```text
liquidation simulation
funding mock
insurance fund placeholder
risk / reconciliation tests
```

## 不在 phase

```text
production liquidation
real funding engine
real insurance fund accounting
production risk controls
```

## 高層 task list

```text
T01 liquidation simulation - completed (pure threshold simulation only)
T02 funding mock - completed (single-scenario batch preview only)
T03 insurance fund placeholder - completed (immutable simulation snapshot only)
T04 risk / reconciliation tests - completed (sandbox conservation check only)
T05 production no-claim review - completed
```

## Sandbox 限制

```text
這只是 simulation only，不是 production liquidation。
risk / liquidation / reconciliation runtime 仍屬 HUMAN_REVIEW_REQUIRED。
```

## HUMAN_REVIEW_REQUIRED

```text
任何 risk / liquidation / reconciliation runtime 變更都屬於 HUMAN_REVIEW_REQUIRED。
任何把 simulation 誤寫成 production liquidation 的行為都屬於 HUMAN_REVIEW_REQUIRED。
```

## T01 implementation notes

- Scope: 建立 `com.lumix.trading.core.futures.sandbox.liquidation` 的 pure、stateless liquidation simulator。它只接收 account-owned open position、人工 mock mark price、明確 simulated collateral 與 maintenance-margin rate。
- Calculation: `simulatedEquity = simulatedCollateral + unrealizedPnL`，其中 LONG PnL 為 `(mark - entry) * quantity`，SHORT PnL 為 `(entry - mark) * quantity`；`simulatedMaintenanceMargin = markPrice * quantity * maintenanceMarginRate`。全程使用 `BigDecimal` 與 `MoneyAmount`，沒有除法、rounding 或 production rate policy。
- Decision: equity 大於 maintenance margin 時回傳 `NOT_LIQUIDATABLE`；小於或等於時回傳 `LIQUIDATION_SIMULATED`。等值採保守 simulation comparison，不是正式強平優先序、破產價、價格保護或風控政策。
- Input guard: account 必須擁有 position、mark price market 必須一致、simulated collateral 不得為負數、maintenance rate 必須大於零且小於一。
- Deliberate boundary: 不產生強平單、不關閉 position、不更新 balance、ledger 或 settlement；不計算 liquidation price、bankruptcy price、maintenance tier、fee、funding、insurance fund、ADL、價格保護、風控限額或任何 persistence / API / Spring runtime。
- Validation commands and result:
  - `cd server && ./mvnw -q -Dtest=FuturesSandboxLiquidationSimulatorTest,P19T01LiquidationSimulationScopeGateTest,FuturesPositionTest,FuturesSandboxMarkPricePnlValuationGateTest test`
  - passed
  - `cd server && ./mvnw test`
  - passed: 285 tests, 0 failures, 0 errors, 2 skipped

## T02 implementation notes

- Scope: `FuturesSandboxFundingMockBatchSimulator` 重用 P18-T05 的單筆 funding preview，建立同一 market、rate 與 funding time 的 batch mock scenario。
- Input guard: 拒絕空集合、重複 position、不同 market、不同 rate 或不同 funding time；避免把不相容資料偽裝成同一 funding cycle。
- Deliberate boundary: 不做 funding payment、跨帳戶 netting、balance/ledger mutation、settlement、schedule 管理或正式 rate/mark-price 管線。

## T03 implementation notes

- Scope: `FuturesSandboxInsuranceFundPlaceholder` 只保留 asset、非負 simulated amount 與 observed time 的 immutable simulation snapshot。
- Deliberate boundary: 不接受注資、賠付、扣款、資產歸集、帳本/餘額異動或 production insurance-fund accounting。

## T04 implementation notes

- Scope: 對稱 LONG/SHORT funding mock scenario 必須使 signed preview 淨額為零，並確認 insurance placeholder 在檢查後保持不變。
- Deliberate boundary: 這是 sandbox conservation check，不是 ledger/balance reconciliation runtime，也不會修正 mismatch 或發動資金操作。

## T05 implementation notes

- Review record: `phase-19-no-claim-review.md` 固定列出 P19 已完成的 simulation 範圍與禁止宣稱。
- No-claim boundary: P19 不代表 production liquidation、formal funding engine、insurance fund accounting、ledger/balance reconciliation runtime、public trading 或 real-money risk control 已完成。
