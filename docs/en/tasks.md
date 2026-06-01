<!-- File purpose: English task directory index. Chinese version: ../zh-TW/tasks.md. -->
# Task Directory

中文版本：[../zh-TW/tasks.md](../zh-TW/tasks.md)

Task files turn roadmap work into selectable Markdown files. When an interrupt request arrives, Codex should convert it into a task file first, then wait for the user to choose which task file to start.

## Entry Points

| Document | Description |
| --- | --- |
| [Active AI Work](../tasks/active.md) | Shared registry for claimed, blocked, and handoff work before implementation starts. |
| [Task README](../tasks/README.md) | Workflow, status legend, and task groups. |
| [Core Kernel README](../tasks/core-kernel/README.md) | Current priority lane for the exchange/matching kernel. |
| [Post-v1 Production Hardening README](../tasks/post-v1/README.md) | Main production-hardening lane after core-v1 freeze. |
| [AI Handoffs](../tasks/handoffs/README.md) | Template for unfinished parallel-agent work. |

## Core Kernel Tasks

| Order | Task | Purpose |
| ---: | --- | --- |
| 1 | [Replayable Matching Core](../tasks/core-kernel/01-replayable-matching-core.md) | Durable command/event log, snapshot, checkpoint, replay validation. |
| 2 | [Liquidation And ADL](../tasks/core-kernel/02-liquidation-adl.md) | Liquidation scanning, execution, ADL ranking, operator controls. |
| 3 | [Bonus Credit And Turnover](../tasks/core-kernel/03-bonus-credit-turnover.md) | Experience fund accounting and turnover tracking. |
| 4 | [Auditable Ledger Reconciliation](../tasks/core-kernel/04-auditable-ledger-reconciliation.md) | Trial balance, immutable journals, exception workflow. |
| 5 | [Market Maker Hedging](../tasks/core-kernel/05-market-maker-hedging.md) | Quoting, inventory, hedge interface, hedge strategy, audit trail. |

## Post-v1 Production Hardening

| Order | Task | Purpose |
| ---: | --- | --- |
| 1 | [Transaction Boundaries](../tasks/post-v1/01-transaction-boundaries.md) | Define MySQL, Redis, Kafka, matching, ledger, and outbox consistency and recovery boundaries. |
| 2 | [Production Worker Routing](../tasks/post-v1/02-production-worker-routing.md) | Route matching commands through the sequencer lease guard and owner epoch. |
| 3 | [ADL Forced Execution](../tasks/post-v1/03-adl-forced-execution.md) | Move ADL from ranking/planning into forced reduction, accounting, and audit. |
| 4 | [Market Data Durability](../tasks/post-v1/04-market-data-durability.md) | Add depth sequence checkpoints, reconnect backfill, and ticker/kline/trade tape durability. |
| 5 | [External API Idempotency](../tasks/post-v1/05-external-api-idempotency.md) | Verify timeout, retry, circuit breaker, rate limit, and idempotency coverage. |

## Web Tasks

| Order | Task | Purpose |
| ---: | --- | --- |
| 1 | [Client Web](../tasks/web/01-client-web.md) | Trading, account, market data, positions, orders, transfers, and user notifications. |
| 2 | [Admin Web](../tasks/web/02-admin-web.md) | Admin dashboard, risk controls, reconciliation, DLQ/recovery, ADL, and market-maker operations. |
