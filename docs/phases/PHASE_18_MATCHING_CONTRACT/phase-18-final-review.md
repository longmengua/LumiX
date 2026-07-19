# Phase 18 - Final Review

## Required Review Fields

```text
Phase: Phase 18 - Futures Trading Sandbox
Task: T01-T06 and phase final review
Scope: 受限單一 market futures sandbox 的 pure boundary 與 inspection eligibility
Files changed: futures order / matching candidate / position snapshot / PnL / mock market-funding / contract gate、對應測試與 scope gates
Tests run: cd server && ./mvnw test
Test result: 279 tests, 0 failures, 0 errors, 2 skipped
Schema changed: no
Money-impacting: no runtime money mutation; futures / PnL / mark price / funding scope requires human review
HUMAN_REVIEW_REQUIRED: yes, approved by human on 2026-07-20
Rollback notes: 本 review 為文件狀態紀錄；若需撤回批准，必須新增 reversal review 記錄並以新的 revert commit 回復對應 task，禁止改寫既有審計歷史
Next task: Phase 19-T01 liquidation simulation
```

## 已批准範圍

```text
T01 futures sandbox LIMIT/GTC order placement
T02 accepted order 的 pure crossed-price candidate evaluation
T03 外部 verified fill 的 one-way open-only position snapshots
T04 unrealized PnL valuation 與 realized PnL close preview
T05 人工 mock mark price 與 funding preview
T06 單一 market contract inspection eligibility gate
```

## 核心審查結論

```text
Phase 18: COMPLETED_FOR_FUTURES_TRADING_SANDBOX_FOUNDATION
Phase 18 human review: APPROVED
Phase 18 人工審核完成
Restricted futures trading sandbox foundation implemented
NOT production-ready
NOT public futures trading ready
NOT real-money ready
NOT matching-execution-ready
NOT fill-producer-ready
NOT balance-reservation-backed
NOT ledger-integrated
NOT settlement-ready
NOT liquidation-ready
NOT formal funding-engine-ready
NOT formal market-data-pipeline-ready
```

## 已驗證的安全邊界

- T01 accepted 只建立 immutable sandbox order，不會進入 order book 或保留保證金。
- T02 `MATCH_ELIGIBLE` 只代表限價條件交叉；不會產生 trade、fill 或 position。
- T03 只接受外部 verified fill，且只建立 one-way、open-only snapshot；不提供 candidate-to-fill conversion。
- T04/T05 只回傳 PnL 與 funding preview；不會更新 balance、ledger 或 settlement。
- T06 只回傳單一 market 的 inspection eligibility；不會呼叫 matching、position update、PnL 或 funding calculator。
- scope gates 已禁止 P18 sandbox source 接入 production market、persistence、transaction、reservation、ledger 與 settlement runtime。

## 仍未完成的 runtime

```text
order book persistence
matching engine execution
trade / fill producer
full position lifecycle: add, reduce, close, reverse, hedge mode
margin reservation / release / capture
balance projection mutation
ledger posting and reconciliation runtime
settlement runtime
maintenance margin / risk ratio / liquidation runtime
formal index and mark-price pipeline
funding interval / rate policy / payment and settlement
fee model and fee collection
API / authorization / public user access
security / monitoring / operations / incident response
real-money futures trading
```

## 禁止誤寫

```text
production-ready
exchange ready
futures trading ready
public futures trading ready
real-money futures ready
order matched
fill created
position opened in runtime
margin reserved
balance frozen
ledger posted
funding settled
settlement completed
liquidation ready
```

## HUMAN_REVIEW_REQUIRED

```text
Approve Phase 18 final review
Phase 18 human review approved
Phase 18 人工審核完成
本輪 P18 scope 的 HUMAN_REVIEW_REQUIRED 已完成人工審核。
後續任何 futures / risk / liquidation / funding / settlement runtime 新變更仍需重新標記 HUMAN_REVIEW_REQUIRED。
```
