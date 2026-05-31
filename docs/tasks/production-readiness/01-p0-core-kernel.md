# P0 Fine Tasks: Core Production Readiness

Status: `tracking`

## Goal

Break the remaining P0 production TODOs into small, commit-sized slices that can move visibly each work session.

## Fine-Grained Progress

`0/36` done.

## Matching And Command Recovery

- [ ] Add a matching/order/account restore drill that starts from snapshot + command/event logs and asserts recovered open orders.
- [ ] Add matching replay validation for multiple symbols with interleaved command offsets.
- [ ] Add restore runbook steps for matching worker takeover after process crash.
- [ ] Add reconnect/session replay semantics for authenticated exchange command clients.
- [ ] Add persistence-backed cancel-replace rollback test covering reserve release and replacement failure.

## ADL And Liquidation Closeout

- [ ] Add ADL execution report API for recent forced-deleveraging outcomes.
- [ ] Add insurance-fund interaction reconciliation for liquidation shortfall and ADL coverage.
- [ ] Add operator runbook for stuck ADL claims, partial retries, and no-candidate retries.
- [ ] Add persistence-backed retry test for ADL queue partial execution after restart.

## Bonus Credit And Turnover Reporting

- [ ] Add exportable bonus-credit campaign report DTO and service method.
- [ ] Add bonus-credit campaign export API and curl script.
- [ ] Add turnover export API for uid/symbol/strategy/market-maker dimensions.
- [ ] Persist first-class strategy/market-maker order tags through order placement and order projection.
- [ ] Add turnover reconciliation report linking order tags to trade tape and ledger refs.

## Auditable Ledger And Finance

- [ ] Add finance exporter job for daily category reports.
- [ ] Add ledger archive manifest restore smoke test.
- [ ] Add database constraints for wallet ledger entry/posting balance invariants where schema can enforce them.
- [ ] Add replay validation test for archived ledger date range.
- [ ] Add operator runbook for unbalanced daily finance report handling.

## Market Maker Quoting And Hedging

- [ ] Add quote state vs open-order reconciliation service.
- [ ] Add quote state reconciliation operator API and curl script.
- [ ] Add per-side quote version metadata for bid/ask replacement history.
- [ ] Add active quote restore test after repository restart/reload.
- [ ] Add real hedge venue adapter skeleton with signed request contract but safe disabled default.
- [ ] Add real hedge venue lookup adapter implementation contract test.
- [ ] Link hedge decisions to internal trade refs when exposure is created from internal fills.
- [ ] Link hedge fills to ledger refs for fee/PnL accounting reconciliation.
- [ ] Add hedge reconciliation issue report for trade-vs-ledger mismatches.

## Transaction Boundaries And Cross-Store Consistency

- [ ] Add persistence-backed rollback test for order place when outbox insert fails.
- [ ] Add persistence-backed rollback test for cancel when ledger release fails.
- [ ] Add persistence-backed rollback test for hedge execution when audit/outbox persistence fails.
- [ ] Add cross-store failure drill doc for MySQL committed but Redis/Kafka delayed.
- [ ] Add recovery command for detecting outbox rows without matching domain-state transition.

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
