# Codex Phase Prompts

## P12 prompt

```text
Read AGENTS.md, AI_AGENT.md, AI_PROGRESS.md, docs/ai/AI_CONTEXT_ROUTING.md, and docs/phases/PHASE_12_DATABASE_SCHEMA/README.md.

Implement only the first incomplete P12 task under docs/phases/PHASE_12_DATABASE_SCHEMA/tasks/.
Do not implement runtime ledger mutation, matching, settlement, deposit crediting, or withdrawal signing.
After changing files, run the narrowest relevant verification and update the task status.
Stop with a review summary.
```

## Review prompt

```text
Review this change against AGENTS.md and docs/PHASE_REVIEW_WORKFLOW.md.
Check for scope creep, phase jumping, money-impacting changes, missing tests, schema risks, and whether HUMAN_REVIEW_REQUIRED is needed.
```
