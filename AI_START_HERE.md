# AI_START_HERE.md

This is the authoritative starting document for Codex work in LumiX.

## Current Authoritative Status

- Phase 11 completed as a documentation-only production architecture reset.
- Phase 12 through Phase 36 are planned and not started.
- The next implementation phase is Phase 12 - Production Database Schema & Migration.
- `docs/OPERATING_EXCHANGE_MASTER_PLAN.md` is the authoritative source of truth.
- `docs/PHASE_REVIEW_WORKFLOW.md` is the authoritative phase-governance source.
- Do not jump phases.
- Do not count interface, stub, mock, placeholder, or TODO work as production completed.
- Do not claim production trading completed before the required readiness gates pass.
- Do not claim production launch ready before Phase 36 and explicit human sign-off.

## Required Read Order Before Any Phase Work

1. `AI_PROGRESS.md`
2. `README.md`
3. `server/README.md`
4. `docs/README.md`
5. `docs/OPERATING_EXCHANGE_MASTER_PLAN.md`
6. `docs/ARCHITECTURE_TEXT_MAP.md`
7. `docs/PHASE_REVIEW_WORKFLOW.md`
8. the current phase file under `docs/phases/`

## Execution Rules

- Reload the repo from disk before each phase.
- Implement only the current approved phase.
- Read the current phase prompt from `docs/phases/`.
- Update `AI_PROGRESS.md` after work.
- Run build and test validation after work.
- Stop after the phase work and wait for human review if the phase is high-risk or if the user requests it.

