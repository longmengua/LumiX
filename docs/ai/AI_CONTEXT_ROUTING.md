# AI 上下文路由

## 任務限定的 AI 施工流程豁免（2026-11-21）

開工時，若使用者明確表示本次任務「可以跳過 AI 施工限制」、「不需要人工批准」或同義授權，agent 直接採用本節，不必再次詢問確認。

- 豁免只適用該任務與同一任務的後續施工；新任務未明確指定時，沿用原有 AI 流程規範，不視為永久取消。
- 豁免期間免逐卡 approve、implementation review 與新增測試事前確認；可直接新增必要測試並回報實際結果。
- 豁免期間不受 phase 順序、task card 或整個 phase 才能交付等流程限制；依實際技術與資料相依完成授權範圍。缺少歷史 phase 文件不作為停工理由。
- `HUMAN_REVIEW_REQUIRED` 保留為審核提示與交付證據，不是施工、測試或完成任務前必須等待的人工批准。
- 豁免不改變技術相依、資料完整性、資金安全與驗證要求，不授權未請求的部署或其他對外操作，也不代表正式上線就緒。
- 本節只在上述明確授權成立時，優先於其他 repo 文件及 skill 中衝突的 AI 施工流程要求；使用者撤回授權後，恢復原流程。

## Routing table

```text
Task type                         Required docs
--------------------------------------------------------------------------------
Any task                          AGENTS.md, AI_AGENT.md, AI_PROGRESS.md
Asset mutation prerequisite       docs/planning/ASSET_RUNTIME_DELIVERY_PROGRAM.md, docs/planning/ASSET_RUNTIME_PREREQUISITE_SEQUENCE.md
Phase 12 schema                   docs/phases/PHASE_12_DATABASE_SCHEMA/README.md
Database migration                docs/phases/PHASE_12_DATABASE_SCHEMA/migration-plan.md
Ledger table design               docs/exchange-core/ledger-invariants.md
Reservation table design          docs/exchange-core/reservation-state-machine.md
Order table design                docs/exchange-core/order-lifecycle.md
Wallet table design               docs/exchange-core/wallet-boundary.md
Backend transaction boundary      server/docs/transaction-boundary.md
API contract                      server/docs/api-contract-guidelines.md
Frontend page work                web/docs/page-map.md
Admin UI / visual refinement      docs/ai/LUMIX_UI_GOVERNANCE.md -> docs/governance/lumix-institutional-blue.md -> web/docs/ui-components.md, web/docs/ui-tokens.md
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
runtime_status: Binance BTC/USDT and ETH/USDT depth and seven approved K-line interval receivers, anonymous read-only push and spot reference UI passed local HTTPS browser visual verification; default disabled except explicit local preview launcher; full market data, production Nginx deployment verification and independent review remain incomplete; production claim prohibited; see server/docs/binance-market-data-boundary.md
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

## Phase 26–36 路由

```text
phase_status: Phase 26 至 Phase 36 均完成 foundation 與 final-review evidence；P36 另完成 readiness documentation，但 runtime 均未開始
current_phase: Phase 36 evidence gap closure after required runtime dependencies
phase_readme: docs/phases/PHASE_36_LAUNCH_GATE/README.md
completed_task_note: docs/phases/PHASE_36_LAUNCH_GATE/phase-36-final-review.md
required_authority: docs/governance/PRODUCTION_READINESS_GATES.md
runtime_status: P36-T01 至 P36-T05 僅完成文件；六類 gate 均為 NOT_READY_EVIDENCE_MISSING，且缺必要 runtime 與人類明確 production sign-off；production-ready 與 launch 均禁止
```
