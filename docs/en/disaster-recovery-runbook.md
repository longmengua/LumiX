<!-- File purpose: production disaster-recovery runbook for matching/order/account/position restore. Chinese version: ../zh-TW/disaster-recovery-runbook.md. -->
# Disaster Recovery Runbook

Use this when restoring matching, orders, accounts, and positions after a process crash, worker takeover, or data-store recovery event.

## Authority Order

1. Matching snapshot plus command/event logs define the order book.
2. Order lifecycle event/projection records define latest order state.
3. Wallet ledger journal and account snapshots define account balances.
4. Position repository and replayed trade events define open positions.
5. Redis hot state is a rebuild target, not the source of truth.

## Matching Worker Takeover

1. Stop traffic for the affected symbol or enable the legacy-routing fence.
2. Acquire a sequencer lease with a higher owner epoch.
3. Call `MatchingRecoveryService.recoverSymbol(symbol)` or start the configured worker.
4. Verify command offset, event offset, match sequence, and validation issues.
5. Run the smoke commands below before reopening traffic.

## Authenticated Command Reconnect

1. Reconnecting clients must keep the same authenticated principal and reuse stable `clientOrderId` / command ids for any command whose previous outcome is unknown.
2. Before resending an effectful submit, cancel, amend, or cancel-replace command, query the order lifecycle projection and recovery consistency reports to determine whether the previous command already reached a terminal or accepted state.
3. Only replay a command when no matching lifecycle projection, command-log entry, or outbox/domain-state transition exists for that client command.
4. Cancel-on-disconnect sessions should reconnect with `resumeConnectionId` before the old close event is processed, then reconcile open orders for the affected uid/symbol before sending new orders.
5. After session replay, run the smoke commands and verify no duplicate client command or missing lifecycle projection issue is reported.

## Smoke Commands

```bash
curl -sS "http://localhost:8080/api/recovery/matching-worker/contexts"
curl -sS "http://localhost:8080/api/recovery/restore/account-position-consistency"
curl -sS "http://localhost:8080/api/recovery/outbox/domain-state-consistency?limit=50"
curl -sS "http://localhost:8080/api/recovery/reconcile/accounts"
```

For each restored symbol, submit a small post-only order, cancel it, and verify lifecycle projection plus outbox consistency before enabling full traffic.

## Pass Criteria

- Matching recovery validation is valid.
- Open orders expected from snapshot plus command log are present.
- Account/position consistency report is valid.
- Outbox/domain-state consistency has no missing lifecycle projection issues.
- Reconciliation reports no account mismatches.
