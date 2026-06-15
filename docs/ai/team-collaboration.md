# Team AI Collaboration

Use this mode when multiple people or agents work in this repository at the same time. The goal is to keep each agent's work reviewable, avoid overwriting another lane, and make unfinished work easy to resume.

For team-level metrics, token budgets, lane sizing, and weekly operating cadence, also use [team-management.md](team-management.md).

## Operating Model

- Work from one Markdown task entry point whenever possible: a file under `docs/tasks/`, `docs/project/todo.md`, `docs/project/todo.md`, or a specific `docs/ai/maps/*.md` file.
- Use `docs/tasks/active.md` as the shared active-work registry before implementation starts.
- Treat one git worktree as one writer lane: one agent, one task entry point, one active registry row, one branch, and one narrow expected file set.
- Do not run two coding agents in the same git worktree. The working tree, index/staging area, uncommitted changes, and generated test state are shared and will make ownership ambiguous.
- For parallel agent work, create separate `git worktree` directories and separate branches before either agent edits files.
- Prefer one agent per task file or one agent per code-map area. If two agents need the same area, split the task into smaller files before both start coding.
- Keep ownership temporary and narrow: own a task, package, or migration while working; do not claim broad directories for longer than needed.
- Treat shared docs as coordination surfaces, not scratchpads. Update them at the end after code and tests are stable.
- Do not revert or rewrite changes from another agent. If a file changed unexpectedly, re-read it and adapt the current patch.

## Worktree Isolation

Use a separate worktree for each parallel writer agent. Example from the primary repo directory:

```bash
git worktree add ../java21-match-hub-agent-a -b agent-a/p1-market-data
git worktree add ../java21-match-hub-agent-b -b agent-b/p1-polymarket
```

Then start each agent with its own `cwd`:

```text
/Users/waltor/project/java21-match-hub-agent-a
/Users/waltor/project/java21-match-hub-agent-b
```

Rules:

- The original repo directory is also a worktree; it may have only one writer agent.
- Each parallel writer agent must commit and push on its own branch.
- `docs/tasks/active.md` coordinates lanes across worktrees and branches only after the claim/completion commits are visible on `main`; branch-only registry rows are not enough for agents that start from `main`.
- Shared external services such as MySQL, Redis, Kafka, and fixed local ports are not isolated by `git worktree`; agents that run integration services must coordinate ports, profiles, and database names.
- Before every new lane or "continue work" start, merge or rebase latest `main` into the agent branch.
- Merge/push the claim commit to `main` before implementation so other agents can see the lane. Merge/push the completion commit back to `main` after focused tests pass and before claiming another lane.

## Multi-Terminal Lane Rules

Use short, stable terminal labels so claims and handoffs are easy to match. Examples:

```text
T1 matching-command-log
T2 polymarket-clob-state
T3 abuse-controls
T4 docs-task-splitter
```

Each terminal agent must follow this contract:

- Own exactly one lane at a time.
- Use its own git worktree and branch when any other writer agent is active.
- Start from exactly one task file, TODO section, or code-map area.
- Register expected files or package areas before editing code.
- Include size, token budget, ETA, risk, and focused tests in the active row when the lane is not trivial.
- Avoid files listed in another active lane's expected areas.
- Do not edit shared coordination docs during implementation unless the lane is specifically about those docs.
- If the lane needs another active lane's files, stop and write the dependency or conflict in `docs/tasks/active.md` or a handoff note.

Good first prompts for a new terminal:

When a user asks how to start parallel work, the answering agent should first read root `README.md`, this file, and `docs/tasks/README.md`, then return copy-paste-ready prompts like the examples below. Do not start implementation while generating these prompts unless the user explicitly asks to start in the same turn.

```text
讀一下 AGENTS.md、docs/ai/team-collaboration.md、docs/tasks/active.md。
認領 docs/tasks/core-kernel/01-replayable-matching-core.md 這條 lane。
先更新 active.md 並 commit/push claim，然後開始實作。
```

```text
Read AGENTS.md, docs/ai/team-collaboration.md, and docs/tasks/active.md.
Claim docs/tasks/post-v1/05-external-api-idempotency.md as one lane.
Commit and push the active.md claim before implementation.
```

Parallel two-terminal prompt examples:

Terminal A:

```text
讀一下 AGENTS.md 和 docs/ai/team-collaboration.md。
我要開 parallel agent A。請建立或使用 worktree ../java21-match-hub-agent-a，branch 用 agent-a/p1-market-data。
不要在目前 worktree 寫 code。
進入該 worktree 後，讀 docs/tasks/active.md，認領 market-data gateway 相關下一個 P1 lane。
先 commit/push active.md claim，再開始實作。
```

Terminal B:

```text
讀一下 AGENTS.md 和 docs/ai/team-collaboration.md。
我要開 parallel agent B。請建立或使用 worktree ../java21-match-hub-agent-b，branch 用 agent-b/p1-polymarket。
不要在目前 worktree 寫 code。
進入該 worktree 後，讀 docs/tasks/active.md，認領 Polymarket integration 相關下一個 P1 lane。
先 commit/push active.md claim，再開始實作。
```

Shorter direct prompts:

```text
以 parallel agent 模式開工。使用獨立 worktree ../java21-match-hub-agent-a 和 branch agent-a/p1-market-data。不要在目前 worktree 寫 code。完成 claim 後從 docs/tasks/production-readiness/02-p1-production-hardening.md 的 Market Data Gateway 下一個未完成項開始做。
```

```text
以 parallel agent 模式開工。使用獨立 worktree ../java21-match-hub-agent-b 和 branch agent-b/p1-polymarket。不要在目前 worktree 寫 code。完成 claim 後從 docs/tasks/production-readiness/02-p1-production-hardening.md 的 Polymarket Integration 下一個未完成項開始做。
```

## Startup Checklist

1. Pull the latest branch state and merge or rebase the latest `main` into this work branch.
2. Confirm this worktree is not being used by another writer agent.
3. Run `./shells/ai-context.sh`.
4. Run `git status --short`.
5. Read `docs/tasks/active.md` and `docs/tasks/handoffs/`.
6. Read the chosen task file or TODO section.
7. Open only the relevant `docs/ai/maps/*.md` file.
8. State the selected lane, worktree path, branch, expected files, and focused tests before editing.

## Claim Before Coding

Use a small claim commit to prevent duplicate starts:

1. Add one row to `docs/tasks/active.md` with `doing`, the task or lane, owner label, date, expected file areas, and handoff link if one exists.
   For non-trivial lanes, include compact management metadata such as `size=M budget=25k-60k eta=0.5d risk=medium`.
2. Commit only the claim row, for example `docs: claim market data gateway work`.
3. Push the claim commit before implementation.
4. Merge or fast-forward the claim commit to `main` and push `main` before implementation.
5. If the push is rejected or the remote registry changed, pull/re-read `docs/tasks/active.md` from latest `main` and choose again.
6. Start coding only after the claim is visible on `origin/main`, not only on the agent branch.

Do not claim broad roadmap buckets such as "P1 hardening". Claim the smallest useful task file, package, or code-map area.

Claim commit messages should stay boring and specific:

```text
docs: claim replayable matching core lane
docs: claim external api idempotency lane
```

## Sync With Main

Parallel branches isolate worktrees; they do not by themselves broadcast coordination state. Every agent must use `main` as the shared registry source:

1. Before starting or continuing with a new lane, merge or rebase latest `main` into the agent branch.
2. After adding a `doing` row to `docs/tasks/active.md`, push that claim and land it on `main` before coding.
3. After finishing the lane, update the registry row to `done` or remove it, push the completion, and land it on `main` before claiming more work.

If `active.md` exists only on an agent branch, other agents starting from `main` may not see it and may duplicate the lane.

## Context Loss Recovery

When an agent resumes after losing chat context:

1. Pull the latest branch state.
2. Read `docs/tasks/active.md`.
3. Read any linked handoff notes.
4. Check whether the claimed task still has uncommitted local changes or a matching recent commit.
5. Continue only if the registry still says the lane is `doing` / `handoff` and no newer completion commit closed it.

If the registry says another owner is `doing`, avoid that lane unless the user explicitly asks to take it over. If a lane appears stale, mark it `handoff` with a note instead of silently replacing the owner.

## Parallel Work Rules

- Parallel writer agents must use separate git worktrees and separate branches.
- Agents must not claim another lane while their previous claim or completion is still branch-only and not visible on `main`.
- Never use `git add .` in a shared worktree while another writer agent is active there; same-worktree parallel writing is disallowed.
- Avoid parallel edits to these high-conflict files unless the task is explicitly about them: `AGENTS.md`, `docs/project/todo.md`, `docs/project/todo.md`, `docs/project/current-state.md`, `docs/project/current-state.md`, `docs/ai/code-map.md`, Flyway migrations, and global config files.
- Avoid overlapping expected areas. If two tasks both need the same controller, service, migration, or map file, split the tasks further or define a dependency order before both agents code.
- If a task needs a schema migration, use the next migration number only after checking `src/main/resources/db/migration`.
- Keep commits or final patch sets scoped to one behavior. Split unrelated cleanup into a later task.
- When adding APIs, update docs and security classification in the same lane.
- When changing core behavior, update the matching `docs/ai/maps/*.md` file before handing off.
- Keep the active registry current: `doing` while coding, `blocked` with a reason when stopped, and `done` or removed when the completion commit lands.

## Handoff Rules

Every agent final response should include:

- Task or lane handled.
- Files changed.
- Tests run, including failures or skipped tests.
- Token usage for the completed fine task or lane. If exact usage is not available from the current interface, write `exact unavailable` and include an estimated range.
- Estimated elapsed time and one context note if discovery was expensive or reusable.
- Remaining TODOs or blockers.

Use this compact format when possible:

```text
Fine task: observability-kafka-lag
Commit: 0aab238
Tests: ./mvnw -Dtest=OperationalMetricsServiceTest test
Token usage: exact unavailable; estimated 25k-40k
```

If work is intentionally left unfinished, create a handoff note under `docs/tasks/handoffs/` using that directory's template. Keep the note short and specific enough that the next agent can continue without rediscovery.

## Conflict Handling

- If `git status --short` shows unrelated changes, leave them alone.
- If another agent changed a file in the current lane, re-read the file before editing and preserve their intent.
- If two active tasks require incompatible edits to the same API, stop and create or update task files that define the split and dependency order.
- If docs and code disagree, trust the task entry point for scope, then update docs once the implementation is verified.
