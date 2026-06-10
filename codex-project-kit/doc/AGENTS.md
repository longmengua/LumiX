# Agent Startup Guide

This file is the first-read context for coding agents in this repository.
Keep it short and update it when ownership, commands, or production TODO sources change.

## Project

Replace this section with the project name, runtime, framework, and high-level purpose.

Primary docs:

- Product overview: `README.md`
- Project structure and naming: `doc/ai/project-structure.md`
- Task intake: `doc/tasks/README.md`
- Active work registry: `doc/tasks/active.md`
- Team collaboration: `doc/ai/team-collaboration.md`
- Team management and usage accounting: `doc/ai/team-management.md`

## Fast Context

Before broad exploration:

```bash
git status --short
find doc -maxdepth 3 -type f | sort
```

Prefer targeted `rg` searches over reading every file.

## Usage Tracking Protocol

When starting a fine task:

```bash
./shells/codex-usage.sh start <task-label>
```

When completing the task:

```bash
./shells/codex-usage.sh end <task-label>
```

If the script cannot read local Codex session logs, report:

```text
Usage delta: unavailable; local Codex token_count log not found.
```

## Task Intake Protocol

When the user points to a Markdown task file:

1. Read that file first.
2. Treat it as the source of truth for the turn.
3. If it names a specific area, open the matching code map or local docs.
4. If it gives one clear next task, implement it without asking for a plan first.
5. If it leaves multiple plausible tasks, ask which one to start.
6. After implementation, update task status, current-state docs, and relevant maps.

## Structure and Naming Protocol

When adding or moving code:

1. Read `doc/ai/project-structure.md` before introducing a new folder, package family, suffix, or ownership boundary.
2. Prefer the narrowest existing location and naming style that already owns the behavior.
3. If the change creates a new convention, update `doc/ai/project-structure.md` in the same task.
4. If the change creates a new functional area, add or update the relevant AI-readable code map or task doc.

## Team AI Collaboration Protocol

When multiple humans or agents are working at the same time:

1. Read `doc/ai/team-collaboration.md` before editing.
2. One git worktree may have only one writer agent.
3. For parallel agent work, use separate `git worktree` directories and branches.
4. Read `doc/tasks/active.md` before choosing work.
5. Claim one lane in `doc/tasks/active.md`, commit, and push the claim before implementation.
6. State selected lane, worktree path, branch, expected files, and focused tests before edits.
7. After a lane is completed, mark it done or remove it in the implementation commit.
8. Leave unfinished work as a short handoff note under `doc/tasks/handoffs/` if that folder exists.

## Common Commands

Replace these with the project commands:

```bash
./gradlew test
npm test
pytest
```

## Commit Protocol

When the user asks to commit, submit, or push completed work:

1. Check `git status --short`.
2. Stage only intended repository changes.
3. Commit with a concise message that describes the main change.
4. Push after a successful commit when the user asks to submit/send out.
5. Report tests that could not run and the exact blocker.

## Fine Task Completion Report

Use this format:

```text
Fine task: <task-label>
Commit: <hash or uncommitted>
Tests: <commands and result>
Usage delta: <output summary from ./shells/codex-usage.sh end task-label>
Remaining: <next task or blocker>
```

## Work Rules

- Do not revert user changes.
- Check `git status --short` before editing.
- Use focused tests for new behavior.
- Update relevant docs when status, ownership, commands, or workflows change.
- Keep folder and file names aligned with `doc/ai/project-structure.md`.
- Add clear comments in every touched code artifact, including application code, tests, SQL migrations, scripts, and frontend code.
- Keep comments focused on intent, state transitions, replay/recovery behavior, accounting effects, risk decisions, security decisions, schema ownership, and test flow.
- Keep docs concise and task-oriented.
- Secrets must come from environment variables or secret managers, not committed config.
