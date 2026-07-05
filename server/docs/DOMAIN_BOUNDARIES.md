# DOMAIN_BOUNDARIES

- `account` is read-only for account views and transfer request modeling.
- `ledger` defines asset-changing interfaces and journal contracts only.
- `idempotency` defines request deduplication contracts only.
- Spot, futures, and margin accounts must remain isolated.
- Business modules must not directly modify `total`, `available`, or `locked`.
- Asset mutations must be routed through the ledger service boundary.
