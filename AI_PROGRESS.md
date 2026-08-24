# AI_PROGRESS.md

## Authoritative status

```text
P-task approval mode: TEMPORARILY_DISABLED_BY_HUMAN_2026-08-24
All future P tasks proceed by phase order and task dependency without per-card approval or implementation-review wait.
This does not authorize prohibited money movement, security bypass, production launch claim, or phase skipping.
```

```text
Phase 11: completed as documentation-only production architecture reset
Phase 12: completed as production database schema and migration foundation
Phase 13: completed as backend module foundation and API boundary
Phase 14: completed as immutable ledger engine foundation, append-only adapter verified on PostgreSQL, not production-ready
Phase 15: COMPLETED_FOR_TRADING_RUNTIME_CORE_FOUNDATION
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
Phase 16: COMPLETED_FOR_SPOT_SANDBOX_FOUNDATION
Spot sandbox flow foundation completed
NOT production-ready
NOT public user trading ready
NOT real-money ready
NOT ledger-posting-integrated
NOT balance-updating
NOT reservation-backed
NOT settlement-finalized
NOT withdrawal-ready
NOT futures/margin/liquidation ready
P16-T10 completed as final review gate only; spot sandbox flow foundation已收斂，但 production runtime 仍未開始
P16-T07 completed as sandbox settlement design gate only; settlement runtime not started
P16-T08 completed as sandbox settlement runtime gate only; ledger posting / balance refresh / reservation commit not started
P16-T09 completed as sandbox settlement / ledger integration design gate only; ledger posting runtime not started
Phase 17: COMPLETED
Phase 17 human review: APPROVED
Phase 17 人工審核完成
Futures core sandbox model foundation implemented
NOT production-ready
NOT public futures trading ready
NOT real-money ready
NOT order-intake-ready
NOT matching-ready
NOT settlement-ready
NOT ledger-integrated
NOT balance-reservation-backed
NOT liquidation-ready
NOT funding-ready
NOT full margin-engine-ready
Phase 18: COMPLETED_FOR_FUTURES_TRADING_SANDBOX_FOUNDATION
Phase 18 human review: APPROVED
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
Phase 19: COMPLETED_FOR_RISK_SANDBOX_FOUNDATION
Phase 19 human review: APPROVED
Risk sandbox foundation implemented
NOT production liquidation ready
NOT formal funding engine ready
NOT insurance fund accounting ready
NOT ledger/balance reconciliation runtime ready
Phase 20: COMPLETED_FOR_CONTRACT_TRADING_INTEGRATION_GATE_FOUNDATION
Phase 20 human review: APPROVED
Contract Trading Integration Gate sandbox foundation implemented
NOT production-ready
NOT formal contract trading launched
NOT public contract trading ready
NOT real-money contract trading ready
NOT matching or fill execution enabled
NOT position, balance or ledger updated
NOT settlement completed
Phase 21: COMPLETED_FOR_MARKET_DATA_FOUNDATION
P21-T01 through P21-T08 completed as immutable market-data domain, admission policy, order-book projection, trade/ticker/candle aggregation, deterministic replay/resync and internal-only query/backpressure foundation
Phase 21 Market Data pipeline runtime, external provider and public transport not started; all P21 work remains pure, in-memory and read-only
NOT official market data service
NOT public market-data API or WebSocket
NOT real-time SLA
NOT production-ready
Phase 22: COMPLETED_FOR_DEPOSIT_OBSERVATION_FOUNDATION
P22-T01 immutable deposit network/address/ownership contract completed; no address provisioning, RPC, schema or credit
P22-T02 provider-neutral chain observation, block/transaction/event identity, deterministic cursor and replay admission contract completed; no RPC, secret, persistence or credit
P22-T03 reorg, confirmation regression, network-isolated halt/resume health-state contract completed; no credit, balance, ledger or provider runtime
P22-T04 read-only reconciliation, deterministic evidence digest, metrics and P23 handoff contract completed; no dashboard, API, persistence or credit
Phase 23: COMPLETED_FOR_DEPOSIT_CREDIT_DECISION_FOUNDATION
P23-T01 immutable deposit candidate, asset/network confirmation policy, versioned eligibility decision contract completed; no credit, ledger or balance mutation
P23-T02 immutable credit idempotency key/decision record and bounded ledger handoff contract completed; no append, balance mutation or persistence runtime
P23-T03 orphaned reorg freeze, append-only reversal ordering and human escalation contract completed; no delete, overwrite or ledger mutation
P23-T04 read-only deposit/ledger/balance reconciliation, exception queue and audit export input contract completed; no repair/admin command or mutation
Phase 24: COMPLETED_FOR_WITHDRAWAL_REQUEST_FOUNDATION
P24-T01 immutable withdrawal request, destination/network, idempotency and request-created audit contract completed; no hold, approval, signing, broadcast or fund movement
P24-T02 immutable eligibility, available-balance evidence, versioned fee quote and future hold handoff contract completed; no hold creation, capture, settlement or mutation
P24-T03 immutable cancel/expire/manual-review/approval-handoff transitions completed; no admin bypass, signing or fund movement
P24-T04 read-only request/hold/audit reconciliation and keyless P25 signer input contract completed; no key, signer command or broadcast
Phase 25: COMPLETED_FOR_WITHDRAWAL_SIGNING_BOUNDARY_FOUNDATION
P25-T01 through P25-T05 completed as immutable approval, signing intent, capability isolation, broadcast evidence and audit/reconciliation contracts; no signer adapter, secret, RPC, broadcast, ledger/balance/reservation mutation or withdrawal completion runtime
Phase 26: COMPLETED_FOR_RISK_CONTROL_POLICY_FOUNDATION
P26 completed as versioned fail-closed limit/freshness/market protection/governance evidence contracts; no freeze, override, consumption, release or production risk runtime
Phase 27: COMPLETED_FOR_ADMIN_CONTROL_BOUNDARY_FOUNDATION
P27 completed as immutable admin session/RBAC, dual-control, read-only source-health and audit export evidence contracts; no admin command, role mutation, UI or bypass
Phase 28: COMPLETED_FOR_AUDIT_COMPLIANCE_EVIDENCE_FOUNDATION
P28 completed as immutable audit metadata, gap gate, minimum-disclosure export and compliance escalation evidence contracts; no export runtime, PII payload, KYC/AML bypass or asset repair
Phase 29: COMPLETED_FOR_API_ADMISSION_CONTRACT_FOUNDATION
P29 completed as version/idempotency/health/rate API admission contracts; no routes, auth runtime, public funds/trading endpoint or transport enforcement
Phase 30: COMPLETED_FOR_TRUSTED_UX_PRESENTATION_FOUNDATION
P30 completed as trusted-data presentation and sensitive enablement contracts; no production adapter, UI execution flow or mock-to-production wiring
Phase 25-36: PLANNING_PROGRAM_DRAFTED
Phase 22-24 detailed task drafts; Phase 25-28 mid-level task breakdowns; Phase 29-36 phase charters drafted
Phase 25-36 runtime implementation not started; proceed only after prerequisite phases and task dependencies are complete
Next implementation phase: Phase 25; proceed by task dependency
```

## 目前倉庫現況

```text
web/    : React + TypeScript + Vite frontend foundation and mock/development adapters
server/ : Java 21 + Spring Boot 3 foundation, interfaces, DTOs, and stubs
docs/   : production architecture and phase planning documents
```

不要把正式帳本引擎、凍結引擎、撮合核心、結算引擎、真實入金系統、真實提款系統或正式行情資料管線視為已完成。

## 目前任務指引

```text
source_of_truth: docs/governance/OPERATING_EXCHANGE_MASTER_PLAN.md
agent_rules: AGENTS.md and AI_AGENT.md
context_router: docs/ai/AI_CONTEXT_ROUTING.md
phase_governance: docs/governance/PHASE_REVIEW_WORKFLOW.md
phase_20_review_status: docs/phases/PHASE_20_FEE_ENGINE/phase-20-final-review.md
current_phase_task_list: docs/phases/PHASE_25_WITHDRAWAL_SIGNING/README.md
next_implementation_phase: Phase 25; proceed by task dependency
completed_task: Phase 21 P21-T01 through P21-T08 market-data foundation; Phase 22 P22-T01 through P22-T04 deposit observation foundation; Phase 23 P23-T01 through P23-T04 deposit credit decision foundation; Phase 24 P24-T01 through P24-T04 withdrawal request foundation
approval_status: P-task approval mode temporarily disabled by human; market-data pipeline runtime/provider/public transport and deposit RPC/credit remain unstarted; production claim prohibited
phase_21_36_planning_program: docs/planning/PHASE_21_36_PLANNING_PROGRAM.md
phase_21_36_review: docs/planning/PHASE_21_36_PLANNING_REVIEW.md
```

## 完成警告

在就緒門檻全部通過前，不要聲稱正式交易已完成。
Do not claim production launch ready before 第 36 階段 and explicit human sign-off.
