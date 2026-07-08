# 第 14 階段 - Immutable Ledger Engine

## 狀態

```text
in progress
```

## 目標

建立 immutable ledger engine 的範圍門檻與 runtime prerequisite，先把 ledger 與其他 bounded context 的交界說清楚，再進入後續 posting runtime。

## 不在範圍內

```text
ledger posting runtime
balance mutation
balance_projections runtime mutation
double-entry posting service
settlement runtime
reservation hold / release runtime
order matching runtime
withdrawal signing / broadcast runtime
Flyway migration 變更
前端變更
```

## 必要閱讀

```text
AGENTS.md
AI_AGENT.md
AI_PROGRESS.md
docs/ai/AI_CONTEXT_ROUTING.md
docs/exchange-core/ledger-invariants.md
docs/backend/transaction-boundary.md
docs/phases/PHASE_13_IDENTITY_ACCOUNT/README.md
docs/phases/PHASE_14_LEDGER_ENGINE/runtime-prerequisites.md
```

## Scope gate

```text
ledger_journals / ledger_entries 是 Phase 12 schema foundation
ledger 是資金真相來源 source of truth
balance_projections 只是 read model
double-entry invariant 必須由 posting service / tests / reconciliation 保證，不是單靠 DB CHECK
append-only policy 後續要由 application rule、permission、trigger 或 operational control 強化
所有 ledger runtime 變更都屬於 HUMAN_REVIEW_REQUIRED
```

## Ledger runtime prerequisites

```text
identity boundary 必須存在且可解析使用者身分
account boundary 必須存在且可驗證 account / account type / account status
asset boundary 必須存在且可驗證 asset symbol / precision / availability
market boundary 必須存在且可驗證 trading symbol / market state / price metadata
Phase 12 ledger schema foundation 必須已存在，包含 journal 與 entry
balance_projections 只能作為可重建的 read model，不能當成資金真相
```

## 任務順序

```text
P14-T01 scope gate and runtime prerequisites
```

## 完成條件

- 只建立 scope gate、runtime prerequisite、boundary skeleton。
- 不實作 ledger posting runtime。
- 不修改 migration。
- 不修改 balance 或 balance_projections。
- 有清楚的 verification method。
- 所有 ledger runtime 變更在規劃與後續實作都要保留 HUMAN_REVIEW_REQUIRED。
