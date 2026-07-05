# Production Readiness Gates

These gates define what must pass before LumiX can make increasingly strong production claims.

## 1. No Production Trading Claim Gate

- Required phases: Phase 11
- Required tests: docs consistency check
- Required evidence: `README.md`, `server/README.md`, `AI_PROGRESS.md`, and roadmap docs explicitly state that production trading is not completed
- Cannot claim before gate passes: production trading completed

## 2. Ledger Correctness Gate

- Required phases: Phase 12, Phase 13, Phase 14
- Required tests: migration tests, balanced-posting tests, idempotency tests, projection rebuild tests, ledger-vs-balance reconciliation tests
- Required evidence: reviewed schema, immutable ledger implementation, projection rebuild evidence, reconciliation reports
- Cannot claim before gate passes: production ledger correctness, production balance mutation correctness

## 3. Fund Freeze Gate

- Required phases: Phase 15
- Required tests: reserve/release/commit/rollback tests, negative-balance prevention tests, partial-fill tests, cancel-release tests, stuck-reservation tests
- Required evidence: reservation state machine, audit trail, failure and retry semantics, reviewer sign-off
- Cannot claim before gate passes: production fund freeze completed

## 4. Matching Determinism Gate

- Required phases: Phase 17, Phase 18
- Required tests: deterministic replay tests, sequence tests, snapshot/recovery tests, duplicate-event handling tests, backpressure and circuit-breaker tests
- Required evidence: benchmark results, protocol documentation, replay evidence, integration test results
- Cannot claim before gate passes: production matching completed

## 5. Settlement Correctness Gate

- Required phases: Phase 19
- Required tests: fill idempotency tests, fee tests, reserve commit/release tests, failed-settlement compensation tests, final-state update tests
- Required evidence: settlement journals, compensation design, reviewed fee model, reconciliation hooks
- Cannot claim before gate passes: production settlement completed

## 6. Wallet Safety Gate

- Required phases: Phase 21, Phase 22, Phase 23
- Required tests: deposit detection and reorg tests, idempotent credit tests, withdrawal approval and release tests, treasury threshold and reconciliation tests
- Required evidence: signer boundary, whitelist and review workflow, treasury alerts, reviewed wallet operational playbooks
- Cannot claim before gate passes: real deposit completed, real withdrawal completed, wallet safety completed

## 7. Reconciliation Gate

- Required phases: Phase 14, Phase 32
- Required tests: ledger-vs-balance, order-vs-trade, matching-event-vs-DB, wallet-vs-chain, fee-revenue reconciliation tests, stuck-state detection tests
- Required evidence: mismatch reports, compensation approval workflow, no-auto-repair control evidence
- Cannot claim before gate passes: production reconciliation completed

## 8. Admin Audit Gate

- Required phases: Phase 25
- Required tests: RBAC tests, approval workflow tests, audit-log tests, reason-code enforcement tests
- Required evidence: immutable admin audit logs, four-eyes approval evidence, least-privilege review
- Cannot claim before gate passes: production admin operations completed

## 9. Risk Kill Switch Gate

- Required phases: Phase 26
- Required tests: user/symbol/global limit tests, halt tests, withdraw-pause tests, global kill-switch tests
- Required evidence: reviewed risk rules, halt propagation proof, audit logs of risk decisions
- Cannot claim before gate passes: production risk controls completed

## 10. Security Gate

- Required phases: Phase 24, Phase 25, Phase 33
- Required tests: abuse-detection tests, security regression checks, dependency audit, alert-generation tests
- Required evidence: threat model, secret-management evidence, open critical finding list, pen-test remediation plan
- Cannot claim before gate passes: security hardening completed, compliance hardening completed

## 11. Observability Gate

- Required phases: Phase 34
- Required tests: telemetry validation, alert validation, runbook drill, dashboard validation
- Required evidence: structured logs, metrics and tracing coverage, on-call runbooks, postmortem template, alert-to-runbook mapping
- Cannot claim before gate passes: production observability and incident readiness completed

## 12. Launch Go/No-Go Gate

- Required phases: Phase 20 through Phase 36, plus all earlier gating phases already passed
- Required tests: launch rehearsal, backup/restore drill, disaster-recovery drill, support escalation drill, final build and release validation
- Required evidence: signed go/no-go package, legal and support readiness, fee and policy docs, market-maker agreement readiness, unresolved-risk register
- Cannot claim before gate passes: production launch ready
