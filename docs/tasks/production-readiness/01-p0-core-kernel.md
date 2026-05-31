# P0 Fine Tasks: Core Production Readiness

Status: `tracking`

## Goal

Break the remaining P0 production TODOs into small, commit-sized slices that can move visibly each work session.

## Fine-Grained Progress

`28/36` done.

## Matching And Command Recovery

- [ ] Add a matching/order/account restore drill that starts from snapshot + command/event logs and asserts recovered open orders.
- [ ] Add matching replay validation for multiple symbols with interleaved command offsets.
- [ ] Add restore runbook steps for matching worker takeover after process crash.
- [ ] Add reconnect/session replay semantics for authenticated exchange command clients.
- [ ] Add persistence-backed cancel-replace rollback test covering reserve release and replacement failure.

## ADL And Liquidation Closeout

- [x] Add ADL execution report API for recent forced-deleveraging outcomes.
- [x] Add insurance-fund interaction reconciliation for liquidation shortfall and ADL coverage.
- [x] Add operator runbook for stuck ADL claims, partial retries, and no-candidate retries.
- [x] Add persistence-backed retry test for ADL queue partial execution after restart.

## Bonus Credit And Turnover Reporting

- [x] Add exportable bonus-credit campaign report DTO and service method.
- [x] Add bonus-credit campaign export API and curl script.
- [x] Add turnover export API for uid/symbol/strategy/market-maker dimensions.
- [x] Persist first-class strategy/market-maker order tags through order placement and order projection.
- [x] Add turnover reconciliation report linking order tags to trade tape and ledger refs.

## Auditable Ledger And Finance

- [x] Add finance exporter job for daily category reports.
- [x] Add ledger archive manifest restore smoke test.
- [x] Add database constraints for wallet ledger entry/posting balance invariants where schema can enforce them.
- [x] Add replay validation test for archived ledger date range.
- [x] Add operator runbook for unbalanced daily finance report handling.

## Market Maker Quoting And Hedging

- [x] Add quote state vs open-order reconciliation service.
- [x] Add quote state reconciliation operator API and curl script.
- [x] Add per-side quote version metadata for bid/ask replacement history.
- [x] Add active quote restore test after repository restart/reload.
- [x] Add real hedge venue adapter skeleton with signed request contract but safe disabled default.
- [x] Add real hedge venue lookup adapter implementation contract test.
- [x] Link hedge decisions to internal trade refs when exposure is created from internal fills.
- [x] Link hedge fills to ledger refs for fee/PnL accounting reconciliation.
- [x] Add hedge reconciliation issue report for trade-vs-ledger mismatches.

## Transaction Boundaries And Cross-Store Consistency

- [x] Add persistence-backed rollback test for order place when outbox insert fails.
- [x] Add persistence-backed rollback test for cancel when ledger release fails.
- [x] Add persistence-backed rollback test for hedge execution when audit/outbox persistence fails.
- [x] Add cross-store failure drill doc for MySQL committed but Redis/Kafka delayed.
- [x] Add recovery command for detecting outbox rows without matching domain-state transition.

## Disaster Recovery

- [ ] Add production disaster-recovery runbook for matching/order/account/position restore.
- [ ] Add smoke test command list for restoring from latest matching snapshot.
- [ ] Add account/position consistency validation report after restore.

## Read First

- [../../en/todo.md](../../en/todo.md)
- [../../ai/maps/order-matching.md](../../ai/maps/order-matching.md)
- [../../ai/maps/risk-ledger-funds.md](../../ai/maps/risk-ledger-funds.md)
- [../../ai/maps/market-maker-hedging.md](../../ai/maps/market-maker-hedging.md)
- [../../ai/maps/persistence-tests.md](../../ai/maps/persistence-tests.md)
