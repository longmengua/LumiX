# Phase 16 - Spot Trading Sandbox

## 狀態

```text
in progress
```

## 目標

建立 sandbox-only spot trading runtime 的 scope gate 與 runtime boundaries。P16-T01 只定義邊界與 safety contract，不代表已經可以做正式交易。

P16-T01 completed as sandbox scope gate and runtime boundary only; runtime implementation not started.
P16-T02 completed as sandbox order intake boundary only; order persistence / matching / settlement not started.
P16-T03 completed as sandbox in-memory order book gate only; matching / settlement not started.
P16-T04 completed as sandbox matching design gate only; matching runtime not started.
P16-T05 completed as sandbox in-memory matching runtime only; settlement / ledger posting not started.
P16-T06 completed as sandbox trade/fill result boundary only; settlement runtime not started.

## Sandbox scope

```text
sandbox only
not production-ready
not public user trading ready
no real money
no external market connectivity
no withdrawal
no futures / margin / liquidation
no production matching engine claim
no production settlement claim
```

## 可串接的後續 boundary

```text
sandbox order intake boundary
sandbox reservation hold/release boundary
sandbox matching boundary
sandbox settlement boundary
ledger posting controlled gate
balance projection rebuild gate
reconciliation boundary
```

## 不允許的 runtime

```text
正式 order placement
正式 matching
正式 settlement
正式 reservation runtime
正式交易上線宣稱
```

## 高層 task list

```text
T01 spot sandbox scope gate and runtime boundaries
T02 sandbox order intake boundary
T03 sandbox in-memory order book gate
T04 sandbox matching design gate
T05 sandbox in-memory matching runtime
T06 sandbox trade/fill result boundary
T07 sandbox settlement boundary
T08 sandbox no-production review gate
```

## 文件索引

```text
docs/phases/PHASE_16_SPOT_TRADING_SANDBOX/tasks/P16-T01.md
docs/phases/PHASE_16_SPOT_TRADING_SANDBOX/tasks/P16-T02.md
docs/phases/PHASE_16_SPOT_TRADING_SANDBOX/tasks/P16-T03.md
docs/phases/PHASE_16_SPOT_TRADING_SANDBOX/tasks/P16-T04.md
docs/phases/PHASE_16_SPOT_TRADING_SANDBOX/tasks/P16-T05.md
docs/phases/PHASE_16_SPOT_TRADING_SANDBOX/tasks/P16-T06.md
```

## Runtime boundary rules

```text
order intake 不得直接寫 ledger 或 balance_projections
matching 不得直接寫 ledger、balance_projections 或 reservations
settlement 必須是 explicit process，並經 ledger posting gate
reservation runtime 未完成前，不得宣稱 available / locked trading-ready
sandbox trade result 不等於 production trade result
requestId 不是 idempotency guarantee
idempotency key 才是 duplicate prevention contract
amount / price / quantity 一律使用 BigDecimal，不得使用 float / double
matching 只做 design gate，不得宣稱已完成正式撮合 runtime
matching / trade / fill 只允許 sandbox in-memory runtime，不得寫 DB
trade/fill result 只代表 settlement input，不代表正式 settlement completed
```

## HUMAN_REVIEW_REQUIRED

```text
所有 money movement / settlement / reservation runtime 都屬於 HUMAN_REVIEW_REQUIRED。
所有 order / matching / settlement / reservation / ledger / balance runtime 都屬於 HUMAN_REVIEW_REQUIRED。
任何把 sandbox 誤寫成正式交易完成的行為都屬於 HUMAN_REVIEW_REQUIRED。
```
