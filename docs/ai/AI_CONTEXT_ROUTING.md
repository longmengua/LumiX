# AI 上下文路由

## Routing table

```text
Task type                         Required docs
--------------------------------------------------------------------------------
Any task                          AGENTS.md, AI_AGENT.md, AI_PROGRESS.md
Phase 12 schema                   docs/phases/PHASE_12_DATABASE_SCHEMA/README.md
Database migration                docs/phases/PHASE_12_DATABASE_SCHEMA/migration-plan.md
Ledger table design               docs/exchange-core/ledger-invariants.md
Reservation table design          docs/exchange-core/reservation-state-machine.md
Order table design                docs/exchange-core/order-lifecycle.md
Wallet table design               docs/exchange-core/wallet-boundary.md
Backend transaction boundary      docs/backend/transaction-boundary.md
API contract                      docs/backend/api-contract-guidelines.md
Frontend page work                docs/frontend/page-map.md
Operations / deployment           docs/operations/deployment-runbook.md
Readiness review                  docs/governance/PRODUCTION_READINESS_GATES.md
Market Data Pipeline              docs/phases/PHASE_21_MARKET_DATA/README.md, currently relevant task card
Deposit Listener                  docs/phases/PHASE_22_DEPOSIT_LISTENER/README.md, currently relevant task card
Phase 21–36 planning              docs/planning/PHASE_21_36_PLANNING_PROGRAM.md, phase README and relevant draft/charter
```

## Token budget rule

```text
Small task      <= 4 docs
Medium task     <= 8 docs
Architecture    <= relevant directory only
Full repo scan   only with explicit human request
```

## 高風險標記條件

下列任務必須標記 `HUMAN_REVIEW_REQUIRED`。逐卡 approve 機制暫停期間，仍要保持 fail-closed、不得實作被 phase 明令禁止的 runtime，並在 phase final review 記錄人類應檢查的項目：

```text
money movement
ledger invariants
withdrawal signing
risk bypass
security controls
fee rounding
chain confirmation policy
admin privileged action
```

## Phase 21 路由

```text
phase: Phase 21 - Market Data Pipeline
phase_readme: docs/phases/PHASE_21_MARKET_DATA/README.md
task_card_review: docs/phases/PHASE_21_MARKET_DATA/phase-21-task-card-review.md
proposed_first_task: docs/phases/PHASE_21_MARKET_DATA/p21-t01-inventory-boundary-invariants.md
approval_status: P-task approval mode temporarily disabled by human; Phase 21 complete as foundation
runtime_status: Market Data pipeline runtime, external provider and public transport not started; P21 only establishes immutable domain, policy, projection, aggregation, replay and internal query foundations; production claim prohibited
completed_task_note: docs/phases/PHASE_21_MARKET_DATA/phase-21-final-review.md
```

## Phase 21–36 規劃路由

```text
planning_program: docs/planning/PHASE_21_36_PLANNING_PROGRAM.md
planning_review: docs/planning/PHASE_21_36_PLANNING_REVIEW.md
phase_22_24: detailed task drafts in each phase README
phase_25_28: mid-level task breakdowns in each phase README
phase_29_36: phase charter and high-level tasks in each phase README
approval_status: P-task approval mode is temporarily disabled by human; phase order, task dependencies and prohibited boundaries remain mandatory
```

## Phase 22 路由

```text
phase: Phase 22 - 入金地址與鏈上監聽器
phase_readme: docs/phases/PHASE_22_DEPOSIT_LISTENER/README.md
current_task: completed
completed_task_note: docs/phases/PHASE_22_DEPOSIT_LISTENER/phase-22-final-review.md
approval_status: P-task approval mode temporarily disabled by human; 仍必須遵守 phase/task dependency
runtime_status: Phase 22 不得建立 RPC/provider 連線、持久化、地址派發、secret handling 或 deposit credit；production claim prohibited
```

## Phase 23 路由

```text
phase: Phase 23 - 入金入帳與確認政策
phase_readme: docs/phases/PHASE_23_DEPOSIT_CREDITING/README.md
current_task: completed
completed_task_note: docs/phases/PHASE_23_DEPOSIT_CREDITING/phase-23-final-review.md
approval_status: P-task approval mode temporarily disabled by human; 仍必須遵守 phase/task dependency
runtime_status: 不得 ledger append、balance mutation、credit/reversal runtime、repair/admin command、provider 連線或 production claim
```

## Phase 24 路由

```text
phase: Phase 24 - 提款請求流程
phase_readme: docs/phases/PHASE_24_WITHDRAWAL_REQUEST/README.md
current_task: completed
completed_task_note: docs/phases/PHASE_24_WITHDRAWAL_REQUEST/phase-24-final-review.md
approval_status: P-task approval mode temporarily disabled by human; 仍必須遵守 phase/task dependency
runtime_status: 沒有 hold 寫入、簽章、廣播、釋放資金、approval bypass、wallet key/secret handling 或 production claim
```

## Phase 25 路由

```text
phase: Phase 25 - 提款審核、簽章、廣播
phase_readme: docs/phases/PHASE_25_WITHDRAWAL_SIGNING/README.md
current_task: completed
completed_task_note: docs/phases/PHASE_25_WITHDRAWAL_SIGNING/phase-25-final-review.md
approval_status: P-task approval mode temporarily disabled by human; 仍必須遵守 phase/task dependency 與高風險 fail-closed 邊界
runtime_status: P24 的 keyless signer input 只是資料契約；不得實作任何可簽章、可廣播或可移動資金的路徑
```
