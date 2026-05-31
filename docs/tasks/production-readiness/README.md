# Production Readiness Fine-Grained Tasks

Status: `tracking`

This group splits the broad `docs/*/todo.md` production checklist into small implementation slices. Use it to track visible progress when a broad P0/P1 checkbox needs several commits before it can be marked done.

## How To Use

1. Pick one unchecked task from the files below.
2. Implement only that slice.
3. Update the task checkbox, `docs/*/todo.md` summary text if the broad item changes, and the relevant `docs/ai/maps/*.md`.
4. Report both broad TODO progress and fine-grained progress, for example:

```text
P0 broad: 31/43 done, remaining 12 -> 12
P0 fine: 0/36 done, remaining 36 -> 35
```

## Fine-Grained Task Files

| File | Priority | Purpose |
| --- | --- | --- |
| [01-p0-core-kernel.md](01-p0-core-kernel.md) | P0 | Matching, ADL, bonus/turnover, ledger, market-maker, transaction, and disaster-recovery slices. |
| [02-p1-production-hardening.md](02-p1-production-hardening.md) | P1 | Market data gateway, Polymarket, database/storage, observability, and alerting slices. |
| [03-p2-evolution.md](03-p2-evolution.md) | P2 | Admin console, reporting, load testing, rollout, and compliance slices. |

## Current Fine-Grained Counts

- P0 fine: `5/36` done.
- P1 fine: `0/22` done.
- P2 fine: `0/14` done.

These counts intentionally start from this split date. Earlier baseline work remains documented in `docs/*/todo.md`, current-state docs, and AI maps.
