# 第 14 階段 - Immutable Ledger Engine

## 狀態

```text
completed
```

## 目標

建立 immutable ledger engine 的範圍門檻與 runtime prerequisite，並在資產 runtime 授權後交付最小但真實的 internal posting service。

## 不在範圍內

```text
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
P14-T02 ledger journal draft and invariant contract
P14-T03 ledger persistence port and append-only mapping contract
P14-T04 ledger posting application command boundary
P14-T05 ledger append transaction boundary design
P14-T06 ledger append persistence adapter implementation gate
P14-T07 PostgreSQL verification for ledger append adapter
P14-T08 ledger idempotency and request identity design gate
P14-T09 ledger runtime integration no-go gate
P14-T10 phase 14 final review gate
```

## 完成條件

- 已建立 scope gate、runtime prerequisite、boundary skeleton。
- 2026-09-15 已交付受控 internal ledger posting runtime；它不提供 HTTP endpoint，也不處理 balance projection、reservation、settlement、入金或提款。
- 不修改 migration。
- 不修改 balance 或 balance_projections。
- 有清楚的 verification method。
- 所有 ledger runtime 變更在規劃與後續實作都要保留 HUMAN_REVIEW_REQUIRED。
- Phase 14 foundation completed，但不是 production-ready，也不是完整 ledger posting runtime。

## 2026-09-15 ASSET-T03 immutable ledger 唯讀 history handoff

`GET /api/v1/assets/history` 僅使用 authenticated session principal，在資料庫層以 `accounts.user_id` 限制既有
`ledger_entries` 與 `ledger_journals` 的讀取範圍。以 `(posted_at, ledger_entry_id)` keyset cursor 回傳 bounded
entry/reference，amount 維持十進位字串；同一 journal 的其他帳戶 entry 不會回傳。此 handoff 沒有 append、修改、
projection rebuild、reservation、入金、提款或資金移動，且不改變 Phase 14 ledger runtime foundation 的完成界線。

## 2026-09-15 資產 runtime：受控帳本入帳服務

`com.lumix.ledger.runtime.TransactionalLedgerPostingService` 是不暴露 HTTP route 的內部 runtime boundary。它在單一
database transaction 內依序完成 durable `LEDGER_POSTING` idempotency claim、double-entry invariant、帳戶／資產／精度
驗證、append-only `ledger_journals`／`ledger_entries`、transactional outbox evidence、immutable audit evidence 與 completed
idempotency resource binding。相同 key 與相同 immutable payload 只回放既有 journal；不同 payload 或尚未完成的 key 一律
fail-closed。它不直接寫 `balance_projections`、不建立 reservation、沒有公開 API，也沒有 provider、wallet、簽章或廣播接線。

`HUMAN_REVIEW_REQUIRED`：審核時必須確認 idempotency hash 對所有 journal / entry / actor 欄位都穩定，及任一後續 SQL
失敗會使 journal、entry、outbox、audit 與 idempotency claim 一併回滾。
