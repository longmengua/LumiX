# Agent Startup Guide

This file is the first-read context for coding agents working in this repository.
Keep it short and update it when package ownership, commands, or production TODO sources change.

## Project

Java21 Match Hub is a Java 21 + Spring Boot backend for an internal exchange core and Polymarket integration.
It is a runnable MVP, not production-grade exchange infrastructure.

Primary docs:
- Product overview: `docs/en/README.md` / `docs/zh-TW/README.md`
- Current state: `docs/en/current-state.md` / `docs/zh-TW/current-state.md`
- Production TODO: `docs/en/todo.md` / `docs/zh-TW/todo.md`
- Agent code map index: `docs/ai/code-map.md`
- Team AI collaboration mode: `docs/ai/team-collaboration.md`

## Fast Context

Run this before broad exploration:

```bash
./shells/ai-context.sh
```

It prints git status, TODO progress, key package paths, and test classes. Prefer that output over reading every Markdown file.

## Work Kickoff Helper Protocol

When the user asks how to start work, says they will open multiple terminals, or asks for parallel-agent instructions:

1. Read root `README.md` first.
2. For task-based work, also read `docs/tasks/README.md`.
3. For parallel-agent work, also read `docs/ai/team-collaboration.md`.
4. Do not start implementation yet unless the user explicitly asks you to start in this same turn.
5. Give the user copy-paste-ready prompts or shell commands for each terminal, including worktree path, branch, lane/task source, claim/push requirement, and the instruction not to write code in the current worktree.

The expected output is practical command text the user can paste into separate AI terminals, not a broad explanation.

## Task Intake Protocol

When the user says `讀一下 <file>.md`, `read <file>.md`, or otherwise points to a Markdown file as the work entry point:

1. Read that file first.
2. Treat it as the task source of truth for the turn.
3. If it names a specific area, open the matching `docs/ai/maps/*.md` sub-map.
4. If it gives one clear next task, implement it without asking for a plan first.
5. If it leaves multiple plausible tasks, ask the user which one to start.
6. Before broad repo exploration, prefer `./shells/ai-context.sh`.
7. After implementation, update `docs/*/todo.md`, `docs/*/current-state.md`, and the relevant `docs/ai/maps/*.md` when the change affects status or code ownership.

Suggested user commands:

```text
讀一下 docs/en/todo.md，從最前面的未完成 P0 開始做。
讀一下 docs/ai/maps/order-matching.md，繼續做 matching 相關 TODO。
讀一下 docs/en/current-state.md，告訴我現在最該先做哪三件事，先不要改 code。
讀一下 AGENTS.md 和 docs/ai/code-map.md，然後繼續 production TODO。
```

## Interrupt Task Protocol

When the user proposes an interrupt priority or says they want tasks split into Markdown files:

1. Convert the request into one or more task files under `docs/tasks/`.
2. Keep each task file scoped to one implementable work item with goal, scope, first implementation slice, acceptance criteria, and read-first links.
3. Update `docs/tasks/README.md` or the relevant group `README.md`.
4. Do not start implementation until the user chooses a task file, unless the user explicitly says to start immediately.

Suggested user commands:

```text
把這個插單需求轉成 task md：先做做市商對沖。
讀一下 docs/tasks/core-kernel/05-market-maker-hedging.md，開始做。
```

## Team AI Collaboration Protocol

When multiple humans or agents are working at the same time:

1. Read `docs/ai/team-collaboration.md` before editing.
2. One git worktree may have only one writer agent. Do not run two coding agents in the same worktree.
3. For parallel agent work, each agent must use a separate `git worktree` directory and a separate branch.
4. Read `docs/tasks/active.md` and existing handoff notes before choosing work.
5. Work from one task file or one `docs/ai/maps/*.md` area at a time.
6. Before each new "continue work" / next-lane start, merge or rebase the latest `main` into the agent branch so the agent sees claims completed by others.
7. Claim the lane in `docs/tasks/active.md`, commit, push that claim, and sync the claim to `main` before implementation.
8. State the selected lane, worktree path, branch, expected files, and focused tests before making edits.
9. Avoid parallel edits to shared coordination files until the end of the task.
10. After a lane is completed, merge/push the completion back to `main` before claiming another lane.
11. Leave unfinished work as a short handoff note under `docs/tasks/handoffs/`.

## Common Commands

```bash
./mvnw test
./mvnw -Dtest=InMemoryMatchingEngineTest test
./mvnw spring-boot:run
docker compose up -d
```

## Commit Protocol

When the user asks to commit, submit, or push completed work:

1. Check `git status --short`.
2. Stage all intended repository changes with `git add .`.
3. Commit with a concise message that describes the main change.
4. Push after a successful commit when the user asks to submit/send out.
5. Report any tests that could not run and the exact blocker.

## Architecture

Dependencies flow from interface adapters toward application/domain contracts:

```text
interfaces.web / interfaces.consumer
  -> application usecase/service/scheduler
  -> domain model/service/event/repository contract
  -> infra adapters for Redis/Kafka/MySQL/matching/HTTP/Web3j
```

Important packages:
- `interfaces.web.controller`: REST API entry points.
- `application.usecase`: request-level business orchestration.
- `application.service`: cross-use-case workflows such as orders, risk, ledger, reconciliation, outbox.
- `domain.model`: orders, accounts, positions, ledger, events, Polymarket models.
- `domain.service`: matching contracts, order book, Polymarket workflows.
- `infra.matching`: current in-memory matching engine.
- `infra.redis` and `domain.repository.jpa`: concrete persistence adapters.

## Work Rules

- Do not revert user changes. Check `git status --short` before editing.
- Use Flyway migrations for schema changes. Hibernate must validate schema, not update it.
- New production-readiness behavior needs focused tests under `src/test/java/com/example/exchange`.
- New API endpoints need docs, curl script coverage when practical, and security classifier review.
- Secrets must come from environment variables or a secret manager, not committed config.
- Keep docs concise; update the relevant `docs/ai/maps/*.md` file when changing core flows or ownership.
- AI-generated or AI-modified code must include clear comments where they help a reader understand intent, state transitions, replay/recovery behavior, accounting effects, risk decisions, or test flow.
- Test code should explain the scenario in comments or `@DisplayName`, so a reader can understand setup, action, and expected result without reverse-engineering the assertions.
- Avoid noisy comments that restate obvious syntax; prefer comments that clarify business rules, invariants, edge cases, and why the test exists.

## Current High-Value TODO Areas

- Durable/replayable matching: command log, event log, snapshots, offset checkpoints, failover rules.
- Transaction boundaries across MySQL, Redis, Kafka, order state, ledger, and outbox.
- Production abuse controls, liquidation scanning, and external API idempotency coverage.
- Market-data persistence, reconnect backfill, and independently scalable gateway/worker processes.
- Polymarket order lifecycle state machine and idempotent CLOB commands.
