# Team Collaboration

Use this mode when multiple people or agents work in the same repository.

## Operating Model

- Work from one task file, TODO section, or code-map area whenever possible.
- One writer agent per worktree.
- Every active lane must be visible in `doc/tasks/active.md`.
- Claims are committed and pushed before implementation.
- Shared coordination files should stay short.

## Claim Flow

1. Pull latest branch state.
2. Read `doc/tasks/active.md`.
3. Choose one unclaimed lane.
4. Add a `doing` row with owner, expected files, focused tests, risk, and timebox.
5. Commit only the claim row.
6. Push the claim.
7. Start implementation.

## Completion Flow

1. Run focused tests.
2. Run broader tests when risk or blast radius justifies it.
3. Update task status and docs.
4. Mark the active row `done` or remove it.
5. Commit implementation.
6. Push when the user asks to submit/send out.
7. Include usage delta from `./shells/codex-usage.sh end <task-label>`.

## Conflict Rules

- If another active lane owns the same files, do not start.
- Split the work or define dependency order.
- If unfinished, add a handoff note with current state, files touched, tests run, and next step.

