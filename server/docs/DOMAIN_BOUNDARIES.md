# DOMAIN_BOUNDARIES

- `account` is read-only for account views and transfer request modeling.
- `ledger` defines asset-changing interfaces and journal contracts only.
- `idempotency` defines request deduplication contracts only.
- Spot and futures accounts are the only product account containers and must remain isolated; futures' internal margin state is not a standalone account type.
- Business modules must not directly modify `total`, `available`, or `locked`.
- Asset mutations must be routed through the ledger service boundary.
