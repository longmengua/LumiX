<!-- File purpose: English task directory index. Chinese version: ../zh-TW/tasks.md. -->
# Task Directory

中文版本：[../zh-TW/tasks.md](../zh-TW/tasks.md)

Task files turn roadmap work into selectable Markdown files. When an interrupt request arrives, Codex should convert it into a task file first, then wait for the user to choose which task file to start.

## Entry Points

| Document | Description |
| --- | --- |
| [Task README](../tasks/README.md) | Workflow, status legend, and task groups. |
| [Core Kernel README](../tasks/core-kernel/README.md) | Current priority lane for the exchange/matching kernel. |

## Core Kernel Tasks

| Order | Task | Purpose |
| ---: | --- | --- |
| 1 | [Replayable Matching Core](../tasks/core-kernel/01-replayable-matching-core.md) | Durable command/event log, snapshot, checkpoint, replay validation. |
| 2 | [Liquidation And ADL](../tasks/core-kernel/02-liquidation-adl.md) | Liquidation scanning, execution, ADL ranking, operator controls. |
| 3 | [Bonus Credit And Turnover](../tasks/core-kernel/03-bonus-credit-turnover.md) | Experience fund accounting and turnover tracking. |
| 4 | [Auditable Ledger Reconciliation](../tasks/core-kernel/04-auditable-ledger-reconciliation.md) | Trial balance, immutable journals, exception workflow. |
| 5 | [Market Maker Hedging](../tasks/core-kernel/05-market-maker-hedging.md) | Quoting, inventory, hedge interface, hedge strategy, audit trail. |

## Web Tasks

| Order | Task | Purpose |
| ---: | --- | --- |
| 1 | [Client Web](../tasks/web/01-client-web.md) | Trading, account, market data, positions, orders, transfers, and user notifications. |
| 2 | [Admin Web](../tasks/web/02-admin-web.md) | Admin dashboard, risk controls, reconciliation, DLQ/recovery, ADL, and market-maker operations. |
