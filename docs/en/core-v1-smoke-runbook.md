<!-- File purpose: English core-v1 smoke-test runbook. Chinese version: ../zh-TW/core-v1-smoke-runbook.md. -->
# Core V1 Smoke Runbook

Use this runbook to verify the bounded core-v1 baseline after checkout, migration, or deployment.

Chinese version: [../zh-TW/core-v1-smoke-runbook.md](../zh-TW/core-v1-smoke-runbook.md)

## Local Dependency Check

```bash
docker compose up -d
docker compose ps
./mvnw test
```

## Configuration Check

- Confirm `spring.flyway.enabled=true` for managed environments.
- Keep state-mutating schedulers disabled by default unless the run explicitly needs them:
  - `BONUS_CREDIT_EXPIRY_ENABLED=false`
  - `MARKET_MAKER_HEDGE_EXECUTION_ENABLED=false`
  - `RISK_SNAPSHOTS_ENABLED=false`
- Confirm emergency switches are understood before enabling traffic:
  - `RISK_CONTROLS_ORDER_ENTRY_HALT`
  - `RISK_CONTROLS_REDUCE_ONLY_MODE`
  - `RISK_CONTROLS_WITHDRAWAL_HALT`
  - `RISK_CONTROLS_LIQUIDATION_HALT`
  - `RISK_CONTROLS_MARKET_MAKER_HEDGE_EXECUTION_HALT`

## API Smoke Areas

- Order entry and matching: place, amend, cancel-replace, cancel, open orders.
- Recovery: matching replay validation and reconciliation report queries.
- Risk: mark price update, account risk, liquidation/ADL controls.
- Ledger: account ledger, trial balance, replay comparison, reconciliation issue workflow.
- Market maker: profile save/query, hedge execution dry path, hedge fill callback, hedge reconciliation.

## Exit Criteria

- Tests pass.
- App starts against local dependencies.
- Protected API paths require admin credentials where expected.
- No scheduler mutates state unexpectedly.
- Known production gaps remain documented in `current-state.md`.
