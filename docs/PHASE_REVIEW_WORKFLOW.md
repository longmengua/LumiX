# Phase Review Workflow

This workflow defines how Codex phases move from implementation to final completion.

## Standard Flow

1. Phase command is issued.
2. Codex reloads the repo from disk.
3. Codex reads the current phase prompt and the phase review workflow.
4. Codex implements only the current phase.
5. Codex validates build and tests.
6. Codex marks the phase as `implementation completed / pending human review`.
7. Human manually reviews the changed files.
8. Human explicitly approves the phase with one of:
   - `Phase X 人工審核完成`
   - `Phase X human review approved`
   - `Approve Phase X completion`
9. Codex updates the phase to `completed` only after the explicit approval.
10. Human sends the Codex output to ChatGPT.
11. ChatGPT verifies and provides the next phase command.

## Codex Must Not

- Auto-mark a phase as completed.
- Jump to the next phase.
- Treat stub, interface, mock, placeholder, or TODO work as production completion.
- Claim production trading completed before the readiness gates pass.
- Claim production launch ready before Phase 36 and explicit sign-off.
- Commit a completed status without an explicit human approval phrase.

## Allowed Maximum Status Before Approval

`implementation completed / pending human review`

## Source of Truth

- `docs/OPERATING_EXCHANGE_MASTER_PLAN.md`
- `docs/ARCHITECTURE_TEXT_MAP.md`
- `docs/phases/`

