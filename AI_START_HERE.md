# AI_START_HERE.md

This is the authoritative starting document for Codex work in LumiX.

## Current Authoritative Status

- Phase 11 completed as production architecture reset documentation only.
- Phase 12 through Phase 36 are planned and not started.
- The next implementation phase is Phase 12 - Production Database Schema & Migration.
- `docs/CODEX_PHASE_PROMPTS.md` is the authoritative prompt source for future phase work.
- Do not jump phases.
- Do not count interface, stub, mock, placeholder, or TODO work as production completed.
- Do not claim production trading completed before the required readiness gates pass.
- Do not claim production launch ready before Phase 36 passes with explicit human sign-off.

## Required Read Order Before Any Phase Work

1. `AI_PROGRESS.md`
2. `README.md`
3. `server/README.md`
4. `docs/PRODUCTION_ROADMAP.md`
5. `docs/PHASE_DEPENDENCY_MAP.md`
6. `docs/PRODUCTION_READINESS_GATES.md`
7. `docs/CODEX_PHASE_PROMPTS.md`
8. the current phase file under `docs/phases/`

## Repo Boundaries

- Frontend stays in `web/` with React + TypeScript + Vite.
- Backend stays in `server/` with Java 21 + Spring Boot 3.
- Production matching must be a C++ core in `core/` or `matching-core/`; Java `MatchingEngineClient` is only an integration boundary.
- Do not implement fake matching, fake order book, fake freeze, fake settlement, fake deposit, or fake withdrawal and count it as complete.
- Do not directly mutate user balances outside the reviewed ledger and reservation architecture.

## Execution Rules

- Reload the repo from disk before each phase.
- Implement only the current approved phase.
- Read the current phase prompt from `docs/CODEX_PHASE_PROMPTS.md`.
- Read the current phase definition from `docs/phases/`.
- Update `AI_PROGRESS.md` after work.
- Run build and test validation after work.
- Stop after the phase work and wait for human review if the phase is high-risk or if the user requests it.

## When The User Says "繼續開工"

1. Reload the repo from disk.
2. Read the required files above.
3. Check `AI_PROGRESS.md` for the next implementation phase.
4. Read the corresponding prompt in `docs/CODEX_PHASE_PROMPTS.md`.
5. Read the matching file in `docs/phases/`.
6. Confirm the current task does not jump phases.
7. Implement only that phase.
8. Run validation commands.
9. Update `AI_PROGRESS.md`.
10. Report changed files, completed scope, remaining scope, validation results, and the next recommended command.
