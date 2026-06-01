# Team AI Collaboration

Use this mode when multiple people or agents work in this repository at the same time. The goal is to keep each agent's work reviewable, avoid overwriting another lane, and make unfinished work easy to resume.

## Operating Model

- Work from one Markdown task entry point whenever possible: a file under `docs/tasks/`, `docs/en/todo.md`, `docs/zh-TW/todo.md`, or a specific `docs/ai/maps/*.md` file.
- Use `docs/tasks/active.md` as the shared active-work registry before implementation starts.
- Prefer one agent per task file or one agent per code-map area. If two agents need the same area, split the task into smaller files before both start coding.
- Keep ownership temporary and narrow: own a task, package, or migration while working; do not claim broad directories for longer than needed.
- Treat shared docs as coordination surfaces, not scratchpads. Update them at the end after code and tests are stable.
- Do not revert or rewrite changes from another agent. If a file changed unexpectedly, re-read it and adapt the current patch.

## Startup Checklist

1. Pull the latest branch state.
2. Run `./shells/ai-context.sh`.
3. Run `git status --short`.
4. Read `docs/tasks/active.md` and `docs/tasks/handoffs/`.
5. Read the chosen task file or TODO section.
6. Open only the relevant `docs/ai/maps/*.md` file.
7. State the selected lane, expected files, and focused tests before editing.

## Claim Before Coding

Use a small claim commit to prevent duplicate starts:

1. Add one row to `docs/tasks/active.md` with `doing`, the task or lane, owner label, date, expected file areas, and handoff link if one exists.
2. Commit only the claim row, for example `docs: claim market data gateway work`.
3. Push the claim commit before implementation.
4. If the push is rejected or the remote registry changed, pull/re-read `docs/tasks/active.md` and choose again.
5. Start coding only after the claim is visible on the remote branch.

Do not claim broad roadmap buckets such as "P1 hardening". Claim the smallest useful task file, package, or code-map area.

## Context Loss Recovery

When an agent resumes after losing chat context:

1. Pull the latest branch state.
2. Read `docs/tasks/active.md`.
3. Read any linked handoff notes.
4. Check whether the claimed task still has uncommitted local changes or a matching recent commit.
5. Continue only if the registry still says the lane is `doing` / `handoff` and no newer completion commit closed it.

If the registry says another owner is `doing`, avoid that lane unless the user explicitly asks to take it over. If a lane appears stale, mark it `handoff` with a note instead of silently replacing the owner.

## Parallel Work Rules

- Avoid parallel edits to these high-conflict files unless the task is explicitly about them: `AGENTS.md`, `docs/en/todo.md`, `docs/zh-TW/todo.md`, `docs/en/current-state.md`, `docs/zh-TW/current-state.md`, `docs/ai/code-map.md`, Flyway migrations, and global config files.
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
- Remaining TODOs or blockers.

If work is intentionally left unfinished, create a handoff note under `docs/tasks/handoffs/` using that directory's template. Keep the note short and specific enough that the next agent can continue without rediscovery.

## Conflict Handling

- If `git status --short` shows unrelated changes, leave them alone.
- If another agent changed a file in the current lane, re-read the file before editing and preserve their intent.
- If two active tasks require incompatible edits to the same API, stop and create or update task files that define the split and dependency order.
- If docs and code disagree, trust the task entry point for scope, then update docs once the implementation is verified.
