# P2 Evolution Tasks

This group turns the P2 roadmap into executable task specs. These tasks are incremental product and operations work after the core production-hardening lanes.

## Task Order

### Admin Console

1. [01-admin-market-config-screen.md](01-admin-market-config-screen.md)
2. [02-admin-risk-parameter-screen.md](02-admin-risk-parameter-screen.md)
3. [03-admin-dlq-replay-screen.md](03-admin-dlq-replay-screen.md)
4. [04-admin-reconciliation-report-screen.md](04-admin-reconciliation-report-screen.md)

### Reporting

5. [05-user-asset-report-service.md](05-user-asset-report-service.md)
6. [06-trade-report-service.md](06-trade-report-service.md)
7. [07-fee-report-service.md](07-fee-report-service.md)
8. [08-operations-finance-daily-report-export.md](08-operations-finance-daily-report-export.md)

### Load Testing

9. [09-order-entry-tps-load-test.md](09-order-entry-tps-load-test.md)
10. [10-matching-tps-load-test.md](10-matching-tps-load-test.md)
11. [11-market-data-fanout-load-test.md](11-market-data-fanout-load-test.md)

### Rollout And Compliance

12. [12-feature-flag-canary-rollback.md](12-feature-flag-canary-rollback.md)
13. [13-kyc-aml-sanctions-integration.md](13-kyc-aml-sanctions-integration.md)
14. [14-trade-surveillance-sar.md](14-trade-surveillance-sar.md)

## Shared Acceptance Rules

- Keep production mutations behind explicit operator permissions and audit logs.
- Prefer read-only/reporting baselines before adding repair or execution actions.
- Do not commit secrets, customer PII samples, or real sanctions/KYC payloads.
- Update `docs/tasks/active.md` and land claims on `origin/main` before implementation.

