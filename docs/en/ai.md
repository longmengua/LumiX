<!-- File purpose: English AI documentation index. Chinese version: ../zh-TW/ai.md. -->
# AI Documentation

中文版本：[../zh-TW/ai.md](../zh-TW/ai.md)

This page links to compact agent-facing maps. The shared source files live under `docs/ai/` so Codex can read only the relevant map for a task.

## Entry Points

| Document | Description |
| --- | --- |
| [AI README](../ai/README.md) | How to ask Codex to use Markdown files as task entry points. |
| [Code Map Index](../ai/code-map.md) | Map directory for task-specific code maps. |
| [Team AI Collaboration](../ai/team-collaboration.md) | Parallel-agent ownership, conflict handling, and handoff rules. |
| [AI Team Management](../ai/team-management.md) | Lane sizing, token budgets, timeboxes, code-map upkeep, and weekly metrics. |

## Code Comment Standard

AI-generated or AI-modified code should include comments that make business intent and test flow clear. Comments should explain state transitions, replay/recovery behavior, accounting effects, risk decisions, invariants, and edge cases. Test code should use comments or `@DisplayName` so setup, action, and expected result are easy to understand.

Avoid comments that only repeat syntax.

## Code Maps

| Area | Link |
| --- | --- |
| Order and matching | [../ai/maps/order-matching.md](../ai/maps/order-matching.md) |
| Risk, ledger, and funds | [../ai/maps/risk-ledger-funds.md](../ai/maps/risk-ledger-funds.md) |
| Reliability and market data | [../ai/maps/reliability-market-data.md](../ai/maps/reliability-market-data.md) |
| Polymarket and security | [../ai/maps/polymarket-security.md](../ai/maps/polymarket-security.md) |
| Market-maker and hedging | [../ai/maps/market-maker-hedging.md](../ai/maps/market-maker-hedging.md) |
| Web applications | [../ai/maps/web-apps.md](../ai/maps/web-apps.md) |
| Persistence and tests | [../ai/maps/persistence-tests.md](../ai/maps/persistence-tests.md) |
