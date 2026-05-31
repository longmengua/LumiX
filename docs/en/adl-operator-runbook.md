<!-- File purpose: operator runbook for ADL queue claims, partial retries, no-candidate retries, and reconciliation. Chinese version: ../zh-TW/adl-operator-runbook.md. -->
# ADL Operator Runbook

This runbook covers ADL queue entries created when liquidation shortfall exceeds insurance-fund coverage.

## Inspect

1. List open ADL shortfalls: `GET /api/risk/adl-queue`.
2. List stuck claims: `GET /api/risk/adl-queue/stuck-claims?minClaimAgeSeconds=900`.
3. Review recent execution outcomes: `GET /api/risk/adl-executions?limit=50`.
4. Reconcile ADL queue against liquidated-position coverage: `GET /api/risk/adl-insurance-reconciliation?asset=USDT`.

## Claim And Execute

1. Claim before manual execution: `POST /api/risk/adl-queue/{liquidationId}/claim`.
2. Execute with a unique `commandId`: `POST /api/risk/adl-queue/{liquidationId}/execute`.
3. Reuse the same `commandId` only when retrying the same uncertain execution; durable execution records make completed commands replay-safe.

## Partial Retry

If execution is partial, the queue amount is reduced to `remainingNotional`. Retry only against the same `liquidationId`; do not enqueue a replacement shortfall manually.

## No Candidate Retry

`ADL_NO_ELIGIBLE_CANDIDATES` leaves the queue unchanged. Retry after profitable opposite-side positions exist or after risk operations decide another manual path.

## Stuck Claims

If the owner is unavailable, release with `POST /api/risk/adl-queue/{liquidationId}/release` using the current owner, then claim with the replacement operator. Keep the audit trail by using a new `commandId` for any new execution attempt.
